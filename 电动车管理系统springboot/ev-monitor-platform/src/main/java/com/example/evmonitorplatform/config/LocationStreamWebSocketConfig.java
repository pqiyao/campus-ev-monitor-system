package com.example.evmonitorplatform.config;

import com.example.evmonitorplatform.websocket.LocationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 原生 WebSocket（纯 JSON），供大屏 index.vue 使用。
 * {@link WebSocketConfig} 中的 /ws/location 为 STOMP+SockJS，不能与 new WebSocket() 直连。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class LocationStreamWebSocketConfig implements WebSocketConfigurer {

    private final LocationWebSocketHandler locationWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(locationWebSocketHandler, "/ws/location/stream")
                .setAllowedOriginPatterns("*");
    }
}
