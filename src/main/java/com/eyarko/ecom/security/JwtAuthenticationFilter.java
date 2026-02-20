package com.eyarko.ecom.security;

import com.eyarko.ecom.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.security.core.Authentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT authentication filter that validates tokens on each protected request.
 * <p>
 * This filter:
 * <ul>
 *   <li>Extracts JWT token from Authorization header (Bearer token)</li>
 *   <li>Validates token signature using HMAC SHA-256</li>
 *   <li>Checks token expiration</li>
 *   <li>Rejects tampered or expired tokens with 401 Unauthorized</li>
 *   <li>Sets authentication context for valid tokens</li>
 * </ul>
 * <p>
 * Error responses:
 * <ul>
 *   <li>Expired token: "Token expired"</li>
 *   <li>Tampered/invalid token: "Invalid or expired token"</li>
 *   <li>Missing token: Request continues (may be handled by authorization rules)</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecurityEventLogger securityEventLogger;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        UserDetailsService userDetailsService,
        TokenBlacklistService tokenBlacklistService,
        SecurityEventLogger securityEventLogger,
        ObjectMapper objectMapper
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.securityEventLogger = securityEventLogger;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String ipAddress = SecurityEventLogger.getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        
        // Check if token is blacklisted (revoked)
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            securityEventLogger.logTokenRevoked(ipAddress, endpoint);
            sendErrorResponse(response, "Token has been revoked", HttpStatus.UNAUTHORIZED);
            return;
        }
        
        try {
            String username = jwtService.extractUsername(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(token, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    // Log successful token validation
                    securityEventLogger.logTokenValid(username, ipAddress, endpoint);
                }
            }
        } catch (ExpiredJwtException ex) {
            // Token expired - return 401 Unauthorized
            securityEventLogger.logTokenExpired(ipAddress, endpoint);
            sendErrorResponse(response, "Token expired", HttpStatus.UNAUTHORIZED);
            return;
        } catch (SignatureException ex) {
            // Token tampered - return 401 Unauthorized
            securityEventLogger.logTokenInvalid(ipAddress, endpoint, "Invalid signature");
            sendErrorResponse(response, "Invalid token signature", HttpStatus.UNAUTHORIZED);
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            // Invalid token format or other JWT error - return 401 Unauthorized
            securityEventLogger.logTokenInvalid(ipAddress, endpoint, ex.getMessage());
            sendErrorResponse(response, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> payload = ApiResponse.<Void>builder()
            .status("error")
            .message(message)
            .build();
        objectMapper.writeValue(response.getOutputStream(), payload);
    }
}

