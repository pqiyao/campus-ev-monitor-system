// config/MqttConfig.java（完整替换）
package com.example.evmonitorplatform.config;

import com.example.evmonitorplatform.service.LocationService;
import com.example.evmonitorplatform.service.ViolationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MqttConfig {

    @Value("${ev-monitor.mqtt.broker}")
    private String broker;
    @Value("${ev-monitor.mqtt.client-id}")
    private String clientId;
    @Value("${ev-monitor.mqtt.topic-location}")
    private String locationTopic;
    @Value("${ev-monitor.mqtt.topic-violation}")
    private String violationTopic;

    // 用字段注入替代 @Bean 参数，避免循环依赖
    private final LocationService locationService;
    private final ViolationService violationService;

    private MqttClient mqttClient;

    @PostConstruct
    public void init() {
        try {
            mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT连接断开: {}", cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    log.debug("MQTT收到消息 topic={}", topic);

                    // 支持多种主题格式：
                    // 1. ev/location/{MAC地址} - 主题中包含MAC
                    // 2. ev/location - 固定主题，MAC在payload中
                    // 3. position - 兼容旧格式
                    if (topic.startsWith("ev/location/") || topic.equals("ev/location") || topic.equals("position")) {
                        String macFromTopic = null;
                        if (topic.startsWith("ev/location/")) {
                            macFromTopic = topic.substring("ev/location/".length());
                        }
                        locationService.handleLocationMessage(topic, payload);
                    } else if (topic.equals(violationTopic)) {
                        violationService.handleMqttViolation(payload);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            mqttClient.connect(options);
            mqttClient.subscribe(locationTopic, 1);
            mqttClient.subscribe(violationTopic, 1);
            log.info("MQTT已连接: {}", broker);

        } catch (MqttException e) {
            // MQTT连接失败不影响整个应用启动
            log.error("MQTT连接失败（应用将继续运行）: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                log.info("MQTT已断开连接");
            }
        } catch (MqttException e) {
            log.error("MQTT断开失败: {}", e.getMessage());
        }
    }
}