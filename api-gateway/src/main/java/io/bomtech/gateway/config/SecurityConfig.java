package io.bomtech.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchange -> exchange
                .pathMatchers("/auth/login", "/auth/register", "/auth/refresh").permitAll()
                .anyExchange().permitAll() // Cho phép mọi request, ủy quyền xác thực cho AuthenticationFilter
            );
        return http.build();
    }
}
