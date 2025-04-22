package io.bomtech.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
        .csrf().disable() // Disable cs
        .authorizeHttpRequests()
            .requestMatchers("/auth/register", "/auth/login", "/auth/refresh").permitAll() // Cho phép endpoint /auth/register không cần xác thực
            .anyRequest().authenticated(); // Các yêu cầu khác cần xác thực
        return http.build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}