// websocket/LocationWebSocketHandler.java
package com.example.evmonitorplatform.websocket;

import com.alibaba.fastjson.JSON;
import com.example.evmonitorplatform.vo.response.VehicleLocationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class LocationWebSocketHandler extends TextWebSocketHandler {

    /** 线程安全的连接列表 */
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("前端连接WebSocket，当前连接数: {}", sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("前端断开WebSocket，当前连接数: {}", sessions.size());
    }

    /** 与 MQTT 推送结构一致的 JSON（供原生 WebSocket 使用） */
    public void broadcastJson(String json) {
        if (sessions.isEmpty() || json == null || json.isEmpty()) return;
        sessions.removeIf(session -> !session.isOpen());
        TextMessage tm = new TextMessage(json);
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) session.sendMessage(tm);
            } catch (Exception e) {
                log.error("WebSocket推送失败: {}", e.getMessage());
            }
        });
    }

    /** MQTT收到新位置时，广播给所有前端 */
    public void broadcastLocation(VehicleLocationVO vo) {
        if (sessions.isEmpty()) return;
        String msg = JSON.toJSONString(vo);
        sessions.removeIf(session -> !session.isOpen());
        sessions.forEach(session -> {
            try {
                session.sendMessage(new TextMessage(msg));
            } catch (Exception e) {
                log.error("WebSocket推送失败: {}", e.getMessage());
            }
        });
    }

    /** 批量广播（前端重连时发送所有当前位置） */
    public void broadcastAll(List<VehicleLocationVO> list) {
        if (sessions.isEmpty() || list.isEmpty()) return;
        String msg = "{\"type\":\"all\",\"data\":" + JSON.toJSONString(list) + "}";
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) session.sendMessage(new TextMessage(msg));
            } catch (Exception e) {
                log.error("批量推送失败: {}", e.getMessage());
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 前端发送"sync"请求时，返回所有当前位置（由Controller处理）
    }
}