package com.example.evmonitorplatform.agent;

import com.example.evmonitorplatform.config.AgentDshProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process manager for the local DeepSeek Harness CLI.
 *
 * <p>
 * When {@code app.agent.dsh.managed=true}, Spring Boot launches
 * {@code npx @deepseek-ai/dsh web
 * --host --port} on {@link ApplicationReadyEvent} and tears it down on
 * {@code @PreDestroy}. The
 * Java process does not call into the Node runtime directly — DSH is still a
 * standalone HTTP
 * server that {@link DshRpcClient} talks to on localhost. This component only
 * owns its process
 * lifecycle and health-check loop so operators do not have to remember to start
 * it separately.
 *
 * <p>
 * Idempotent: if DSH is already reachable on {@code host:port} when
 * {@link #start()} runs, we
 * do nothing and treat the pre-existing process as the source of truth.
 */
@Component
@Slf4j
public class DshProcessManager implements ApplicationListener<ApplicationReadyEvent> {

    private final AgentDshProperties properties;

    private final HttpClient healthClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(1_500))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final AtomicReference<Process> processRef = new AtomicReference<>();
    private final AtomicReference<Instant> startedAt = new AtomicReference<>();
    /** Rolling last 20 lines of DSH stdout (merged with stderr). */
    private final List<String> tail = new ArrayList<>();
    private final Object tailLock = new Object();
    /** Launch token extracted from the DSH startup URL (query param). */
    private final AtomicReference<String> launchToken = new AtomicReference<>();

    public DshProcessManager(AgentDshProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!properties.isEnabled() || !properties.isManaged()) {
            log.info("DSH 托管未启用（enabled={}, managed={}），跳过自动启动",
                    properties.isEnabled(), properties.isManaged());
            return;
        }
        try {
            start();
        } catch (RuntimeException ex) {
            log.warn("DSH 自动启动失败，Agent 工作台仍可使用手动启动的 DSH 实例: {}", ex.getMessage());
        }
    }

    public synchronized void start() {
        if (isHealthy()) {
            log.info("DSH 已在 {}:{}} 运行，跳过托管启动", properties.getHost(), properties.getPort());
            startedAt.compareAndSet(null, Instant.now());
            return;
        }
        if (processRef.get() != null && processRef.get().isAlive()) {
            log.info("DSH 进程已存在（pid={}），等待健康检查", processRef.get().pid());
        }
        String npx = resolveExecutable(properties.getNpxPath(), "npx.cmd", "npx");
        if (npx == null) {
            throw new IllegalStateException("未找到 npx，请安装 Node.js 或在 app.agent.dsh.npxPath 显式指定路径");
        }
        List<String> cmd = buildCommand(npx);
        log.info("启动 DSH: {}", String.join(" ", cmd));
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workspaceDirectory());
            pb.redirectErrorStream(true);
            File log = resolveLogFile();
            pb.redirectOutput(log);
            pb.redirectError(log);
            Process proc = pb.start();
            processRef.set(proc);
            startedAt.set(Instant.now());
            startTailReader(proc);
        } catch (IOException ex) {
            throw new IllegalStateException("DSH 进程启动失败: " + ex.getMessage(), ex);
        }
        waitUntilReady();
    }

    @PreDestroy
    public synchronized void shutdown() {
        Process proc = processRef.getAndSet(null);
        if (proc == null)
            return;
        log.info("停止 DSH 进程 pid={}", proc.pid());
        proc.destroy();
        try {
            boolean exited = proc.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!exited) {
                log.warn("DSH 未在 5 秒内退出，强制 kill");
                proc.destroyForcibly();
                proc.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            proc.destroyForcibly();
        }
        startedAt.set(null);
    }

    /** Returns a snapshot useful for {@link DshRpcClient#status()} enrichment. */
    public Map<String, Object> statusSnapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        Process proc = processRef.get();
        if (proc == null) {
            out.put("managedProcess", false);
        } else {
            out.put("managedProcess", true);
            out.put("alive", proc.isAlive());
            out.put("pid", proc.pid());
            out.put("upSince", startedAt.get() == null ? null : startedAt.get().toString());
            try {
                if (proc.isAlive()) {
                    out.put("uptimeMs", System.currentTimeMillis() - proc.info().startInstant()
                            .map(i -> i.toEpochMilli()).orElse(System.currentTimeMillis()));
                } else {
                    out.put("exitCode", proc.exitValue());
                }
            } catch (UnsupportedOperationException | IllegalStateException ignored) {
                // Windows 下 proc.info() 可能不返回 startInstant
            }
        }
        synchronized (tailLock) {
            out.put("lastLogTail", List.copyOf(tail));
        }
        return out;
    }

    /**
     * Launch token extracted from DSH's startup URL (query param). Returns null if
     * not yet captured.
     */
    public String getLaunchToken() {
        return launchToken.get();
    }

    public boolean isHealthy() {
        // DSH 所有 HTTP endpoint 都会返回 401 Unauthorized 除非带 token。
        // A 401 (client error, 4xx) means the process is alive and serving requests —
        // only
        // 5xx / connect-timeout / connection-refused / DNS error would indicate the
        // service
        // has not bound yet or has crashed.
        String url = "http://" + properties.getHost() + ":" + properties.getPort() + "/";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(1_500))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> resp = healthClient.send(req, HttpResponse.BodyHandlers.discarding());
            int code = resp.statusCode();
            boolean healthy = code >= 200 && code < 500;
            if (!healthy) {
                log.debug("DSH health check returned HTTP {} (unhealthy)", code);
            }
            return healthy;
        } catch (Exception ex) {
            log.debug("DSH health check failed: {}", ex.getMessage());
            return false;
        }
    }

    // ---------------- internals ----------------

    private List<String> buildCommand(String npx) {
        List<String> cmd = new ArrayList<>();
        cmd.add(npx);
        cmd.add(properties.getNpmPackage());
        cmd.add("--profile");
        cmd.add(properties.getProfile());
        cmd.add("--host");
        cmd.add(properties.getHost());
        cmd.add("--port");
        cmd.add(String.valueOf(properties.getPort()));
        cmd.add("--no-open");
        // Bypass browser-trust fence for local RPC from Spring Boot.
        cmd.add("--trusted-host");
        cmd.add(properties.getHost() + ":" + properties.getPort());
        if (StringUtils.hasText(properties.getExtraArgs())) {
            for (String token : properties.getExtraArgs().trim().split("\\s+")) {
                cmd.add(token);
            }
        }
        return cmd;
    }

    private void waitUntilReady() {
        long deadline = System.currentTimeMillis() + Math.max(5_000, properties.getMaxStartupMs());
        int interval = Math.max(500, properties.getHealthCheckMs());
        long lastAlive = System.currentTimeMillis();
        while (System.currentTimeMillis() < deadline) {
            Process p = processRef.get();
            if (p == null || !p.isAlive()) {
                throw new IllegalStateException("DSH 进程意外退出，无法等待健康检查就绪。请查看日志: " + resolveLogFile());
            }
            if (isHealthy()) {
                log.info("DSH 已就绪 (pid={})", p.pid());
                return;
            }
            if (System.currentTimeMillis() - lastAlive > 30_000) {
                lastAlive = System.currentTimeMillis();
                log.info("DSH 尚未就绪，继续等待... (已用时 {}ms)",
                        properties.getMaxStartupMs() - (deadline - System.currentTimeMillis()));
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待 DSH 启动被中断", ex);
            }
        }
        throw new IllegalStateException("DSH 在 " + properties.getMaxStartupMs() + "ms 内未就绪，放弃托管启动");
    }

    private File resolveLogFile() {
        if (StringUtils.hasText(properties.getLogFile())) {
            return new File(properties.getLogFile());
        }
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".ev-monitor", "dsh");
            Files.createDirectories(dir);
            return dir.resolve("dsh.log").toFile();
        } catch (IOException ex) {
            return new File(System.getProperty("java.io.tmpdir"), "dsh.log");
        }
    }

    private File workspaceDirectory() {
        File dir = StringUtils.hasText(properties.getWorkspaceRoot())
                ? new File(properties.getWorkspaceRoot())
                : new File(System.getProperty("user.dir"));
        if (!dir.isDirectory()) {
            log.warn("DSH 工作目录不存在，回退到 user.dir: {}", dir);
            dir = new File(System.getProperty("user.dir"));
        }
        return dir;
    }

    /**
     * Find the first matching executable on {@code PATH}.
     * On Windows we prefer {@code npx.cmd}/{@code node.exe} to the bare names
     * because
     * ProcessBuilder does not resolve PATHEXT the way cmd.exe does.
     */
    private static String resolveExecutable(String explicit, String... candidates) {
        if (StringUtils.hasText(explicit))
            return explicit;
        String path = System.getenv("PATH");
        String[] dirs = (path == null ? "" : path).split(File.pathSeparator);
        for (String dir : dirs) {
            for (String candidate : candidates) {
                File f = new File(dir, candidate);
                if (f.isFile())
                    return f.getAbsolutePath();
            }
        }
        return null;
    }

    private void startTailReader(Process proc) {
        File logFile = resolveLogFile();
        Thread reader = new Thread(() -> {
            long lastOffset = 0;
            while (proc.isAlive()) {
                try {
                    long size = logFile.length();
                    if (size > lastOffset) {
                        try (BufferedReader br = new BufferedReader(
                                new java.io.InputStreamReader(
                                        new java.io.FileInputStream(logFile), StandardCharsets.UTF_8))) {
                            // Seek to lastOffset by skipping chars, then read new lines.
                            br.skip(lastOffset);
                            String line;
                            while ((line = br.readLine()) != null) {
                                synchronized (tailLock) {
                                    tail.add(line);
                                    if (tail.size() > 200)
                                        tail.remove(0);
                                }
                                // Extract launch token from URLs like
                                // dsh web: http://127.0.0.1:8091/?token=xxxx...
                                if (launchToken.get() == null) {
                                    int q = line.indexOf("token=");
                                    if (q >= 0) {
                                        String after = line.substring(q + 6);
                                        StringBuilder tok = new StringBuilder();
                                        for (int j = 0; j < after.length(); j++) {
                                            char c = after.charAt(j);
                                            if (Character.isLetterOrDigit(c) || c == '_' || c == '-')
                                                tok.append(c);
                                            else
                                                break;
                                        }
                                        if (tok.length() >= 10) {
                                            launchToken.set(tok.toString());
                                            log.info("DSH launch token captured: {}...",
                                                    tok.substring(0, Math.min(6, tok.length())));
                                        }
                                    }
                                }
                            }
                        }
                        lastOffset = size;
                    }
                    Thread.sleep(500);
                } catch (IOException ex) {
                    // File might be rotated or inaccessible briefly.
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "dsh-log-tail");
        reader.setDaemon(true);
        reader.start();
    }
}
