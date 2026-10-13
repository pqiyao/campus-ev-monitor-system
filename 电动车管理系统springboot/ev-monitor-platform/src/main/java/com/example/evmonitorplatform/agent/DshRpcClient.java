package com.example.evmonitorplatform.agent;

import com.example.evmonitorplatform.config.AgentDshProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Allow-listed HTTP adapter for the local DSH RPC endpoint.
 *
 * <p>DSH 0.2.0-rc.2 protects {@code /api/*} with two layers:
 * <ol>
 *   <li>browser-trust fence on the Host header — bypassed by launching DSH with
 *       {@code --trusted-host 127.0.0.1:8091} (see
 *       {@link DshProcessManager#buildCommand}).</li>
 *   <li>persistent signed cookie derived from the process launch token —
 *       obtained by visiting {@code /?token=<launchToken>} and reusing the
 *       {@code Set-Cookie: dsh-auth-*} header on every RPC call.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DshRpcClient {

    private static final Set<String> READ_METHODS = Set.of("session.list", "session.history", "session.models");
    private static final Set<String> WRITE_METHODS = Set.of("session.create", "session.prompt");

    private final AgentDshProperties properties;
    private final ObjectMapper objectMapper;
    private final DshProcessManager processManager;

    /** Signed cookie cached after the first token exchange; volatile for cross-thread visibility. */
    private volatile String authenticatedCookie;

    public Map<String, Object> read(String method, Map<String, Object> payload) {
        if (!READ_METHODS.contains(method))
            throw new IllegalArgumentException("DSH 只读方法未授权: " + method);
        return call(method, payload, 10_000);
    }

    public Map<String, Object> write(String method, Map<String, Object> payload) {
        if (!WRITE_METHODS.contains(method))
            throw new IllegalArgumentException("DSH 写方法未授权: " + method);
        return call(method, payload, Math.max(properties.getTimeoutMs(), 30_000));
    }

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>(processManager.statusSnapshot());
        String url = effectiveBaseUrl();
        result.put("enabled", properties.isEnabled());
        result.put("managed", properties.isManaged());
        result.put("baseUrl", url);
        result.put("host", properties.getHost());
        result.put("port", properties.getPort());
        result.put("profile", properties.getProfile());
        if (!properties.isEnabled()) {
            result.put("state", "DISABLED");
            result.put("message", "DSH 未启用");
            return result;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(Math.max(500, properties.getTimeoutMs())))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<Void> response = client().send(request, HttpResponse.BodyHandlers.discarding());
            result.put("httpStatus", response.statusCode());
            result.put("state", response.statusCode() < 500 ? "READY" : "UNAVAILABLE");
            if (response.statusCode() >= 500)
                result.put("message", "DSH HTTP 5xx");
        } catch (Exception ex) {
            result.put("state", "UNAVAILABLE");
            result.put("message", "DSH Web 未连接: " + ex.getMessage());
        }
        return result;
    }

    private String effectiveBaseUrl() {
        return "http://" + properties.getHost() + ":" + properties.getPort();
    }

    private Map<String, Object> call(String method, Map<String, Object> payload, int timeoutMs) {
        if (!properties.isEnabled())
            throw new IllegalStateException("DSH 运行时未启用");
        try {
            // Make sure we have a valid auth cookie before the first API hit.
            ensureAuthenticated();

            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("type", "client-request");
            envelope.put("rpcId", "campus-agent-" + UUID.randomUUID());
            envelope.put("method", method);
            envelope.put("payload", payload == null ? Map.of() : payload);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(effectiveBaseUrl() + "/api/" + method))
                    .timeout(Duration.ofMillis(Math.max(500, timeoutMs)))
                    .header("Content-Type", "application/json");
            if (authenticatedCookie != null) reqBuilder.header("Cookie", authenticatedCookie);

            HttpRequest request = reqBuilder.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(envelope))).build();
            HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());

            // 401 = cookie expired/was rejected; clear it, re-authenticate, retry once.
            if (response.statusCode() == 401 && authenticatedCookie != null) {
                log.debug("DSH cookie rejected (401), refreshing and retrying {}", method);
                authenticatedCookie = null;
                ensureAuthenticated();
                reqBuilder = HttpRequest.newBuilder(URI.create(effectiveBaseUrl() + "/api/" + method))
                        .timeout(Duration.ofMillis(Math.max(500, timeoutMs)))
                        .header("Content-Type", "application/json");
                if (authenticatedCookie != null) reqBuilder.header("Cookie", authenticatedCookie);
                request = reqBuilder.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(envelope))).build();
                response = client().send(request, HttpResponse.BodyHandlers.ofString());
            }

            if (response.statusCode() >= 400)
                throw new IllegalStateException("DSH RPC HTTP " + response.statusCode());

            Map<String, Object> parsed = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            Object result = parsed.get("result");
            if (result instanceof Map<?, ?> resultMap && Boolean.FALSE.equals(resultMap.get("ok"))) {
                Object error = resultMap.get("error");
                Object message = error instanceof Map<?, ?> errorMap ? errorMap.get("message") : null;
                throw new IllegalStateException(String.valueOf(message == null ? "DSH 请求失败" : message));
            }
            return parsed;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("DSH RPC 调用失败: " + method, ex);
        }
    }

    /**
     * Exchange the DSH launch token for a persistent signed cookie.
     *
     * <p>DSH issues a {@code Set-Cookie: dsh-auth-xxx=...} header on a GET to
     * the index URL when the {@code ?token=<launchToken>} query param matches
     * the process's launch token. After capture, every subsequent RPC carries
     * that cookie.
     */
    private void ensureAuthenticated() {
        if (authenticatedCookie != null) return;
        String token = processManager.getLaunchToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "DSH launch token 尚未捕获，请检查 DSH 是否已启动且日志中包含 token=... URL");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(effectiveBaseUrl() + "/?token=" + token))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("DSH token exchange HTTP " + response.statusCode());
            }
            String setCookie = response.headers().firstValue("Set-Cookie").orElse(null);
            if (setCookie == null) {
                // DSH may also embed multiple Set-Cookie headers — try "cookie" key.
                List<String> allCookieHeaders = response.headers().allValues("Set-Cookie");
                if (!allCookieHeaders.isEmpty()) setCookie = allCookieHeaders.get(0);
            }
            if (setCookie == null) {
                throw new IllegalStateException("DSH token exchange 返回无 Set-Cookie");
            }
            // DSH sets: dsh-auth-xxx=v1.body.sig; Max-Age=...; Path=/; ...
            int eq = setCookie.indexOf('=');
            int semi = setCookie.indexOf(';');
            if (eq <= 0 || semi <= eq) {
                throw new IllegalStateException("DSH Set-Cookie 格式异常: " + setCookie);
            }
            authenticatedCookie = setCookie.substring(0, semi).trim();
            log.info("DSH 认证 cookie 已获取 (len={})", authenticatedCookie.length());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("DSH token exchange 失败", ex);
        }
    }

    private HttpClient client() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(Math.max(500, properties.getTimeoutMs()))).build();
    }
}
