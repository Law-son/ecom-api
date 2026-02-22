package com.eyarko.ecom.config;

import com.eyarko.ecom.security.JwtAuthenticationFilter;
import com.eyarko.ecom.security.JwtProperties;
import com.eyarko.ecom.security.CustomOAuth2UserService;
import com.eyarko.ecom.security.CustomOidcUserService;
import com.eyarko.ecom.security.OAuth2AuthenticationSuccessHandler;
import com.eyarko.ecom.security.RestAccessDeniedHandler;
import com.eyarko.ecom.security.RestAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
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
 * <p>
 * <b>CSRF Protection:</b>
 * <ul>
 *   <li>CSRF is <b>disabled</b> for stateless JWT-based API endpoints</li>
 *   <li>This is appropriate because:
 *     <ul>
 *       <li>JWT tokens are sent in Authorization headers (not cookies)</li>
 *       <li>API is stateless (no server-side sessions)</li>
 *       <li>Same-origin policy and CORS protect against CSRF attacks</li>
 *     </ul>
 *   </li>
 *   <li>CSRF <b>should be enabled</b> for:
 *     <ul>
 *       <li>Stateful session-based authentication (cookies)</li>
 *       <li>HTML form submissions (application/x-www-form-urlencoded)</li>
 *       <li>Browser-based applications using session cookies</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>
 * A demonstration form endpoint with CSRF enabled is available at `/api/v1/demo/form-submit`
 * to show how CSRF protection works with form submissions.
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
     * <b>Password Security with Hashing:</b>
     * <ul>
     *   <li>Passwords are hashed using BCrypt before storage (never stored in plain text)</li>
     *   <li>BCrypt uses adaptive hashing with salt generation</li>
     *   <li>Work factor (default 10) makes brute-force attacks computationally expensive</li>
     *   <li>Each password hash includes a unique salt, preventing rainbow table attacks</li>
     * </ul>
     * <p>
     * BCrypt is used for:
     * <ul>
     *   <li>Storing password hashes in the database (never plain text)</li>
     *   <li>Verifying passwords during authentication (matches plain text against hash)</li>
     *   <li>Encoding passwords when users register or update their password</li>
     * </ul>
     * <p>
     * <b>Security Features:</b>
     * <ul>
     *   <li>One-way hashing: passwords cannot be reversed from hashes</li>
     *   <li>Salt per password: prevents rainbow table attacks</li>
     *   <li>Adaptive cost: can increase work factor as hardware improves</li>
     * </ul>
     *
     * @return BCrypt password encoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2SecurityFilterChain(
        HttpSecurity http,
        CustomOAuth2UserService customOAuth2UserService,
        CustomOidcUserService customOidcUserService,
        OAuth2AuthenticationSuccessHandler successHandler,
        CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
            // Only OAuth2 endpoints (Google login + callback)
            .securityMatcher("/oauth2/**", "/login/oauth2/**")
            // CSRF must be disabled for OAuth2 login endpoints
            // OAuth2 uses redirects and state parameters for security instead of CSRF tokens
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)  // For standard OAuth2
                    .oidcUserService(customOidcUserService) // For OIDC (when using "openid" scope)
                )
                .successHandler(successHandler)
            )
            // OAuth2 uses a session for the authorization request; allow session creation when needed.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        RestAuthenticationEntryPoint authenticationEntryPoint,
        RestAccessDeniedHandler accessDeniedHandler,
        CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
            // CSRF is disabled for stateless JWT APIs
            // JWT tokens in Authorization headers are not vulnerable to CSRF attacks
            // CORS configuration provides cross-origin protection
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
                .requestMatchers(ADMIN_ENDPOINTS).hasAnyRole("ADMIN", "STAFF")
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

    /**
     * Security filter chain for form endpoints with CSRF protection enabled.
     * <p>
     * This filter chain demonstrates CSRF protection for form-based endpoints.
     * CSRF is enabled here to show how it works with form submissions.
     * <p>
     * This configuration uses:
     * <ul>
     *   <li>Stateful sessions (for CSRF token storage)</li>
     *   <li>CSRF protection enabled</li>
     *   <li>Form-based authentication disabled (we use JWT for API)</li>
     * </ul>
     * <p>
     * Order is set to 2 (lower priority) so the main JWT filter chain (order 1) takes precedence.
     *
     * @param http HTTP security configuration
     * @return security filter chain with CSRF enabled
     * @throws Exception if configuration fails
     */
    @Bean
    @Order(2)
    public SecurityFilterChain formSecurityFilterChain(HttpSecurity http) throws Exception {
        // CSRF token request attribute handler for modern browsers
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
            .securityMatcher("/api/v1/demo/**")  // Only apply to demo endpoints
            .csrf(csrf -> {
                csrf.csrfTokenRequestHandler(requestHandler);
                org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository tokenRepository = 
                    new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository();
                tokenRepository.setHeaderName("X-CSRF-TOKEN");
                csrf.csrfTokenRepository(tokenRepository);
            })
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)  // Create session for CSRF token
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/demo/**").permitAll()  // Public demo endpoints
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}

