package com.eyarko.ecom.config;

import com.eyarko.ecom.security.JwtAuthenticationFilter;
import com.eyarko.ecom.security.JwtProperties;
import com.eyarko.ecom.security.RestAccessDeniedHandler;
import com.eyarko.ecom.security.RestAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration defining access policies for all endpoints.
 * <p>
 * Endpoints are categorized as:
 * <ul>
 *   <li>Public: No authentication required (login, registration, catalog browsing)</li>
 *   <li>Authenticated: Requires valid JWT token (customer operations)</li>
 *   <li>Admin: Requires ADMIN role (management operations)</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    /**
     * Public endpoints that require no authentication.
     * Includes authentication endpoints (login, refresh) and user registration.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/login",   // Login endpoint
        "/api/v1/auth/refresh",  // Refresh token endpoint
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/actuator/health",
        "/actuator/info",
        "/graphiql",
        "/graphiql/**"
    };

    /**
     * Public GET endpoints for catalog browsing.
     * Products, categories, and reviews can be viewed without authentication.
     */
    private static final String[] PUBLIC_GET_ENDPOINTS = {
        "/api/v1/products/**",
        "/api/v1/categories/**",
        "/api/v1/reviews/**"
    };

    /**
     * User registration endpoint (POST only).
     * Registration is public to allow new users to create accounts.
     */
    private static final String USER_REGISTRATION_ENDPOINT = "/api/v1/users";

    /**
     * Authenticated endpoints requiring valid JWT token.
     * These endpoints are accessible to both CUSTOMER and ADMIN roles.
     */
    private static final String[] AUTHENTICATED_ENDPOINTS = {
        "/api/v1/cart/**",      // Cart operations
        "/graphql"              // GraphQL queries/mutations
    };

    /**
     * Admin-only endpoints requiring ADMIN role.
     * These endpoints are restricted to administrative operations.
     */
    private static final String[] ADMIN_ENDPOINTS = {
        "/api/v1/inventory/**"  // Inventory management
    };

    /**
     * Password encoder using BCrypt hashing algorithm.
     * <p>
     * BCrypt is used for:
     * <ul>
     *   <li>Storing password hashes in the database (never plain text)</li>
     *   <li>Verifying passwords during authentication</li>
     *   <li>Encoding passwords when users register or update their password</li>
     * </ul>
     * <p>
     * BCrypt automatically handles salt generation and uses a work factor
     * (default 10) to make brute-force attacks computationally expensive.
     *
     * @return BCrypt password encoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        RestAuthenticationEntryPoint authenticationEntryPoint,
        RestAccessDeniedHandler accessDeniedHandler,
        CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints: login, registration, catalog browsing
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.POST, USER_REGISTRATION_ENDPOINT).permitAll()

                // Authenticated endpoints: require valid JWT (CUSTOMER or ADMIN)
                .requestMatchers(AUTHENTICATED_ENDPOINTS).authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()  // Logout endpoint
                .requestMatchers(HttpMethod.POST, "/api/v1/orders").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/*/status").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*/status").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/reviews").authenticated()

                // Admin-only endpoints: require ADMIN role
                .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/orders/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, USER_REGISTRATION_ENDPOINT).hasRole("ADMIN")
                .requestMatchers(USER_REGISTRATION_ENDPOINT + "/**").hasRole("ADMIN")

                // Default: deny all other requests
                .anyRequest().denyAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}

