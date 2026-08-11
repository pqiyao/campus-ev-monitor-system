//package com.example.evmonitorplatform.config;
//
//import com.example.evmonitorplatform.entity.PassRecord;
//import com.example.evmonitorplatform.service.PassRecordService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.EnableAsync;
//import com.alibaba.fastjson2.JSONObject;
//
//import jakarta.annotation.PostConstruct;
//import java.io.*;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//@Slf4j
//@Configuration
//@RequiredArgsConstructor
//@EnableAsync
//public class TcpServerConfig {
//
//    private final PassRecordService passRecordService;
//
//    @Value("${ev-monitor.tcp.port:9999}")
//    private int tcpPort;
//
//    private ExecutorService executor;
//    private ServerSocket serverSocket;
//    private volatile boolean running = false;
//
//    @PostConstruct
//    public void init() {
//        log.info("【终极兼容TCP】配置加载完成，端口：{}", tcpPort);
//    }
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void startTcpServer() {
//        if (running) return;
//
//        executor = Executors.newFixedThreadPool(5);
//        running = true;
//
//        executor.execute(() -> {
//            try {
//                serverSocket = new ServerSocket(tcpPort);
//                log.info("【终极兼容TCP】服务已启动，监听端口：{}", tcpPort);
//
//                while (running) {
//                    try {
//                        Socket client = serverSocket.accept();
//                        log.info("新设备连接：{}", client.getInetAddress());
//                        executor.execute(() -> handleClient(client));
//                    } catch (IOException e) {
//                        if (running) log.error("接收连接异常", e);
//                    }
//                }
//            } catch (IOException e) {
//                log.error("TCP 服务启动失败", e);
//            }
//        });
//    }
//
//    /**
//     * 【改造重点】
//     * 只要收到数据就立刻打印，不等待流关闭
//     * 兼容所有硬件设备：发多少读多少，实时处理
//     */
//    private void handleClient(Socket clientSocket) {
//        try (InputStream in = clientSocket.getInputStream();
//             OutputStream out = clientSocket.getOutputStream()) {
//
//            // 关键：设置短超时，不会一直卡死
//            clientSocket.setSoTimeout(3000);
//            byte[] buffer = new byte[2048];
//            int len;
//
//            // 一读到数据就立即处理
//            while ((len = in.read(buffer)) != -1) {
//                String rawData = new String(buffer, 0, len, StandardCharsets.UTF_8).trim();
//                if (!rawData.isEmpty()) {
//                    // ✅ 只要发了数据，这里立刻打印！
//                    log.info("【实时接收数据】：{}", rawData);
//
//                    // 解析 + 保存
//                    PassRecord record = parseUniversalData(rawData);
//                    if (record != null) {
//                        passRecordService.savePassRecord(record);
//                        log.info("【保存成功】卡号：{}，方向：{}", record.getEtcNumber(), record.getDirection());
//                    }
//
//                    // 回复 OK
//                    out.write("OK\n".getBytes(StandardCharsets.UTF_8));
//                    out.flush();
//                }
//            }
//
//        } catch (Exception e) {
//            // 正常超时，不打印错误
//            if (!e.getMessage().contains("Read timed out")) {
//                log.error("处理客户端数据异常", e);
//            }
//        } finally {
//            try {
//                clientSocket.close();
//            } catch (Exception ignored) {}
//        }
//    }
//
//    private PassRecord parseUniversalData(String rawData) {
//        try {
//            PassRecord record = new PassRecord();
//
//            // JSON 格式
//            if (rawData.startsWith("{") && rawData.endsWith("}")) {
//                JSONObject json = JSONObject.parseObject(rawData);
//                String cardNo = json.getString("D");
//
//                if (cardNo == null || cardNo.isBlank()) {
//                    log.warn("JSON 格式未找到 D 字段（卡号）");
//                    return null;
//                }
//
//                record.setEtcNumber(cardNo);
//                record.setDirection("in");
//                record.setPassResult("pass");
//                record.setPassTime(LocalDateTime.now());
//                return record;
//            }
//
//            // 文本格式
//            else {
//                String[] parts = rawData.split("[,;|\\s]+");
//                for (String part : parts) {
//                    part = part.trim();
//                    if (part.length() < 2) continue;
//
//                    if (part.matches("[0-9A-Fa-f]{6,}")) {
//                        record.setEtcNumber(part);
//                    } else if (part.equalsIgnoreCase("in") || part.equalsIgnoreCase("out")) {
//                        record.setDirection(part.toLowerCase());
//                    } else if (part.equalsIgnoreCase("pass") || part.equalsIgnoreCase("deny")) {
//                        record.setPassResult(part.toLowerCase());
//                    } else if (part.matches("R\\d+")) {
//                        record.setReaderId(part);
//                    }
//                }
//
//                if (record.getDirection() == null) record.setDirection("in");
//                if (record.getPassResult() == null) record.setPassResult("pass");
//                record.setPassTime(LocalDateTime.now());
//
//                return record.getEtcNumber() != null ? record : null;
//            }
//
//        } catch (Exception e) {
//            log.error("数据解析失败：{}", rawData, e);
//            return null;
//        }
//    }
//
//    public void stopTcpServer() {
//        running = false;
//        try {
//            if (serverSocket != null) serverSocket.close();
//            if (executor != null) executor.shutdown();
//            log.info("【终极兼容TCP】服务已停止");
//        } catch (IOException e) {
//            log.error("关闭TCP失败", e);
//        }
//    }
//}