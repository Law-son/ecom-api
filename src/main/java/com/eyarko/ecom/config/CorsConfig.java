package com.eyarko.ecom.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Global CORS configuration integrated with Spring Security.
 * <p>
 * This configuration:
 * <ul>
 *   <li>Defines allowed origins, methods, and headers</li>
 *   <li>Rejects unauthorized origins automatically</li>
 *   <li>Supports credentials for authenticated requests</li>
 *   <li>Is configurable via application properties</li>
 * </ul>
 * <p>
 * To test CORS:
 * <ul>
 *   <li>Use Postman with Origin header set to an allowed origin</li>
 *   <li>Test from a web frontend (e.g., React app on localhost:5173)</li>
 *   <li>Verify unauthorized origins are rejected (403 Forbidden)</li>
 * </ul>
 */
@Configuration
public class CorsConfig {
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    /**
     * Creates a CORS configuration source for Spring Security.
     * This bean is used by SecurityFilterChain to enforce CORS policies.
     * Unauthorized origins are automatically rejected by Spring Security.
     *
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from comma-separated string
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList());
        
        // Parse allowed methods
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        configuration.setAllowedMethods(methods.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList());
        
        // Parse allowed headers (support wildcard)
        if ("*".equals(allowedHeaders.trim())) {
            configuration.addAllowedHeader("*");
        } else {
            List<String> headers = Arrays.asList(allowedHeaders.split(","));
            configuration.setAllowedHeaders(headers.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        }
        
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);
        
        // Apply to all API endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/graphql", configuration);
        
        return source;
    }
}

