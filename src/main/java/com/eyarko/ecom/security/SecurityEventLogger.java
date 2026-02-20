package com.eyarko.ecom.security;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Service for logging security events including authentication and access patterns.
 * <p>
 * This service logs:
 * <ul>
 *   <li>Authentication success and failure events</li>
 *   <li>Token usage patterns</li>
 *   <li>Endpoint access frequency</li>
 *   <li>Suspicious activity indicators</li>
 * </ul>
 * <p>
 * Logs are structured for easy parsing and analysis to detect:
 * <ul>
 *   <li>Brute-force attacks (multiple failed login attempts)</li>
 *   <li>Unusual access patterns</li>
 *   <li>Token misuse</li>
 *   <li>Unauthorized access attempts</li>
 * </ul>
 */
@Component
public class SecurityEventLogger {
    private static final Logger logger = LoggerFactory.getLogger(SecurityEventLogger.class);
    private static final String AUTH_SUCCESS = "AUTH_SUCCESS";
    private static final String AUTH_FAILURE = "AUTH_FAILURE";
    private static final String TOKEN_VALID = "TOKEN_VALID";
    private static final String TOKEN_INVALID = "TOKEN_INVALID";
    private static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    private static final String TOKEN_REVOKED = "TOKEN_REVOKED";
    private static final String ENDPOINT_ACCESS = "ENDPOINT_ACCESS";
    private static final String LOGOUT = "LOGOUT";

    /**
     * Logs a successful authentication event.
     *
     * @param email user email
     * @param ipAddress client IP address
     * @param userAgent client user agent
     */
    public void logAuthenticationSuccess(String email, String ipAddress, String userAgent) {
        logger.info("SECURITY_EVENT: {} | email={} | ip={} | userAgent={} | timestamp={}",
            AUTH_SUCCESS, email, ipAddress, userAgent, Instant.now());
    }

    /**
     * Logs a failed authentication event.
     * <p>
     * Multiple failures from the same IP/email may indicate brute-force attacks.
     *
     * @param email attempted email (may be null)
     * @param ipAddress client IP address
     * @param userAgent client user agent
     * @param reason failure reason
     */
    public void logAuthenticationFailure(String email, String ipAddress, String userAgent, String reason) {
        logger.warn("SECURITY_EVENT: {} | email={} | ip={} | userAgent={} | reason={} | timestamp={}",
            AUTH_FAILURE, email != null ? email : "unknown", ipAddress, userAgent, reason, Instant.now());
    }

    /**
     * Logs successful token validation.
     *
     * @param email user email from token
     * @param ipAddress client IP address
     * @param endpoint accessed endpoint
     */
    public void logTokenValid(String email, String ipAddress, String endpoint) {
        logger.debug("SECURITY_EVENT: {} | email={} | ip={} | endpoint={} | timestamp={}",
            TOKEN_VALID, email, ipAddress, endpoint, Instant.now());
    }

    /**
     * Logs invalid token usage (tampered or malformed).
     *
     * @param ipAddress client IP address
     * @param endpoint accessed endpoint
     * @param reason failure reason
     */
    public void logTokenInvalid(String ipAddress, String endpoint, String reason) {
        logger.warn("SECURITY_EVENT: {} | ip={} | endpoint={} | reason={} | timestamp={}",
            TOKEN_INVALID, ipAddress, endpoint, reason, Instant.now());
    }

    /**
     * Logs expired token usage.
     *
     * @param ipAddress client IP address
     * @param endpoint accessed endpoint
     */
    public void logTokenExpired(String ipAddress, String endpoint) {
        logger.warn("SECURITY_EVENT: {} | ip={} | endpoint={} | timestamp={}",
            TOKEN_EXPIRED, ipAddress, endpoint, Instant.now());
    }

    /**
     * Logs revoked/blacklisted token usage.
     *
     * @param ipAddress client IP address
     * @param endpoint accessed endpoint
     */
    public void logTokenRevoked(String ipAddress, String endpoint) {
        logger.warn("SECURITY_EVENT: {} | ip={} | endpoint={} | timestamp={}",
            TOKEN_REVOKED, ipAddress, endpoint, Instant.now());
    }

    /**
     * Logs endpoint access for frequency tracking.
     *
     * @param email user email (null for unauthenticated)
     * @param ipAddress client IP address
     * @param method HTTP method
     * @param endpoint accessed endpoint
     * @param statusCode HTTP status code
     */
    public void logEndpointAccess(String email, String ipAddress, String method, String endpoint, int statusCode) {
        logger.info("SECURITY_EVENT: {} | email={} | ip={} | method={} | endpoint={} | status={} | timestamp={}",
            ENDPOINT_ACCESS, email != null ? email : "anonymous", ipAddress, method, endpoint, statusCode, Instant.now());
    }

    /**
     * Logs user logout event.
     *
     * @param email user email
     * @param ipAddress client IP address
     */
    public void logLogout(String email, String ipAddress) {
        logger.info("SECURITY_EVENT: {} | email={} | ip={} | timestamp={}",
            LOGOUT, email, ipAddress, Instant.now());
    }

    /**
     * Extracts client IP address from request.
     * Handles proxy headers (X-Forwarded-For, X-Real-IP).
     *
     * @param request HTTP request
     * @return client IP address
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Extracts user agent from request.
     *
     * @param request HTTP request
     * @return user agent string
     */
    public static String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }
}

