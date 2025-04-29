package io.bomtech.device.config;

import io.bomtech.device.websocket.DeviceWebSocketHandler;
import io.bomtech.device.websocket.WebUpdatesWebSocketHandler; // Import new handler
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

    // Inject both handlers
    private final DeviceWebSocketHandler deviceWebSocketHandler;
    private final WebUpdatesWebSocketHandler webUpdatesWebSocketHandler; // Inject new handler

    public WebSocketConfig(DeviceWebSocketHandler deviceWebSocketHandler, WebUpdatesWebSocketHandler webUpdatesWebSocketHandler) { // Update constructor
        this.deviceWebSocketHandler = deviceWebSocketHandler;
        this.webUpdatesWebSocketHandler = webUpdatesWebSocketHandler; // Assign new handler
    }

    @Bean
    public HandlerMapping handlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/device", deviceWebSocketHandler); // Mobile devices connect here
        map.put("/ws/web/updates", webUpdatesWebSocketHandler); // Web clients connect here

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
