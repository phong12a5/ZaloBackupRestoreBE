package io.bomtech.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // Use new lambda DSL configurers
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.reactive.function.client.WebClient; // Keep if needed for other purposes

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF
            .csrf(AbstractHttpConfigurer::disable)
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Permit access to registration, login, and refresh endpoints without authentication
                .requestMatchers("/auth/register", "/auth/login", "/auth/refresh").permitAll()
                // Require authentication for any other request (if any exist in this service)
                .anyRequest().authenticated()
            );
        return http.build();
    }

    // Keep WebClient if it's used elsewhere in auth-service
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}