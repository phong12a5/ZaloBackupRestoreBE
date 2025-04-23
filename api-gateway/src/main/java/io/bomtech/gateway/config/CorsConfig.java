package io.bomtech.gateway.config; // Correct package name

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    // Using CorsWebFilter (often preferred with Spring Cloud Gateway)
    @Bean
    CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        // Allow frontend origin - Adjust port if your frontend runs elsewhere
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost")); // Allow requests from frontend
        corsConfig.setMaxAge(3600L); // Cache preflight response for 1 hour
        corsConfig.addAllowedMethod("*"); // Allow all standard methods (GET, POST, PUT, DELETE, OPTIONS)
        corsConfig.addAllowedHeader("*"); // Allow all headers
        // corsConfig.setAllowCredentials(true); // Uncomment if frontend needs to send cookies

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig); // Apply CORS configuration to all paths ("/**")

        return new CorsWebFilter(source);
    }

    /*
    // Alternative using WebFluxConfigurer (might be less effective with Gateway filters)
    // @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply to all paths
                .allowedOrigins("http://localhost") // Allow frontend origin (adjust port if different)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed methods
                .allowedHeaders("*") // Allow all headers
                // .allowCredentials(true) // Allow credentials (cookies, authorization headers)
                .maxAge(3600); // Cache preflight response for 1 hour
    }
    */
}
