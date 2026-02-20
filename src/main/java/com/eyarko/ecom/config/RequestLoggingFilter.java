package com.eyarko.ecom.config;

import com.eyarko.ecom.security.SecurityEventLogger;
import com.eyarko.ecom.security.SecurityMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Request logging filter that tracks endpoint access for security monitoring.
 * <p>
 * This filter:
 * <ul>
 *   <li>Logs all HTTP requests with method, URI, status, and duration</li>
 *   <li>Tracks endpoint access frequency for security reports</li>
 *   <li>Records access patterns for authenticated and anonymous users</li>
 * </ul>
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final SecurityEventLogger securityEventLogger;
    private final SecurityMetricsService securityMetricsService;

    public RequestLoggingFilter(
        SecurityEventLogger securityEventLogger,
        SecurityMetricsService securityMetricsService
    ) {
        this.securityEventLogger = securityEventLogger;
        this.securityMetricsService = securityMetricsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String method = request.getMethod();
            String endpoint = request.getRequestURI();
            int statusCode = response.getStatus();
            
            // Log request details
            logger.info(
                    "{} {} -> {} ({} ms)",
                    method,
                    endpoint,
                    statusCode,
                    duration
            );
            
            // Track endpoint access for security metrics
            String email = getAuthenticatedUserEmail();
            String ipAddress = SecurityEventLogger.getClientIpAddress(request);
            securityEventLogger.logEndpointAccess(email, ipAddress, method, endpoint, statusCode);
            securityMetricsService.recordEndpointAccess(email, ipAddress, endpoint);
        }
    }
    
    /**
     * Extracts authenticated user email from security context.
     *
     * @return user email or null if not authenticated
     */
    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        return null;
    }
}

