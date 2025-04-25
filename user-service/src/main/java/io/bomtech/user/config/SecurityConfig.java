package io.bomtech.user.config; // Adjust package if needed

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF as this is likely a stateless API service
            .csrf(AbstractHttpConfigurer::disable)
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Require authentication for all requests to this service
                .anyRequest().authenticated()
            );
            // Since authentication is handled by the Gateway,
            // we don't need formLogin, httpBasic, etc., here.
            // The service assumes that if a request reaches it (and isn't public),
            // it has already been authenticated by the Gateway.

        return http.build();
    }
}
