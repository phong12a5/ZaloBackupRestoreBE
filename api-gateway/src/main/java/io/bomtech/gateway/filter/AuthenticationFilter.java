package io.bomtech.gateway.filter; // Adjust package if needed

import io.bomtech.gateway.util.JwtUtil; // Adjust import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    // Define public paths that don't require authentication
    private final List<String> publicApiEndpoints = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/refresh"
            // Add other public paths if needed
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Check if the path is public
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange); // Skip authentication for public paths
        }

        // Check for Authorization header
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return onError(exchange, HttpStatus.UNAUTHORIZED); // No Authorization header
        }

        String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String jwt = null;

        // Check if header starts with "Bearer "
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
        } else {
             return onError(exchange, HttpStatus.UNAUTHORIZED); // Invalid header format
        }

        // Validate the JWT
        if (jwt == null || !jwtUtil.validateToken(jwt)) {
             return onError(exchange, HttpStatus.UNAUTHORIZED); // Invalid or expired token
        }

        // Optional: Add username or claims to request headers for downstream services
        String username = jwtUtil.getUsernameFromToken(jwt);
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
               .header("X-User-Name", username)
               .build();
        ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
        return chain.filter(modifiedExchange);

    }

    private boolean isPublicEndpoint(String path) {
        return publicApiEndpoints.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

     @Override
     public int getOrder() {
         // Run before other filters like routing filters
         return -100; // High precedence
     }
}
