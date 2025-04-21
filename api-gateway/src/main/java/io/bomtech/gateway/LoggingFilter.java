package io.bomtech.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        System.out.println("Request Path: " + request.getPath());
        System.out.println("Request Headers: " + request.getHeaders());

        return chain.filter(exchange).doOnSuccess(aVoid -> {
            ServerHttpResponse response = exchange.getResponse();
            System.out.println("Response Status Code: " + response.getStatusCode());
        });
    }

    @Override
    public int getOrder() {
        return -1; // Ensures this filter runs first
    }
}