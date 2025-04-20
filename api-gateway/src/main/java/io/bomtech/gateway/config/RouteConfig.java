package io.bomtech.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route to auth-service
                .route("auth-service", r -> r.path("/auth/**")
                        .uri("http://auth-service:8081"))
                // Route to user-service
                .route("user-service", r -> r.path("/users/**")
                        .uri("http://user-service:8082"))
                .build();
    }
}