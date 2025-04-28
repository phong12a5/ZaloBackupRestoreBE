package io.bomtech.device.config;

import io.bomtech.device.websocket.DeviceWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {

    // Inject the handler bean
    private final DeviceWebSocketHandler deviceWebSocketHandler;

    public WebSocketConfig(DeviceWebSocketHandler deviceWebSocketHandler) {
        this.deviceWebSocketHandler = deviceWebSocketHandler;
    }

    @Bean
    public HandlerMapping handlerMapping() {
        // Map the WebSocket endpoint URL to our handler
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/device", deviceWebSocketHandler); // Mobile devices will connect to this endpoint

        // Use SimpleUrlHandlerMapping for WebSocket mapping
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1); // Give precedence to WebSocket mapping
        return mapping;
    }

    // Required adapter for WebSocket handling with WebFlux
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
