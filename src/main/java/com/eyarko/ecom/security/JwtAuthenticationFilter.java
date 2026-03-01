package com.eyarko.ecom.security;

import com.eyarko.ecom.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecurityEventLogger securityEventLogger;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        TokenBlacklistService tokenBlacklistService,
        SecurityEventLogger securityEventLogger,
        ObjectMapper objectMapper
    ) {
        this.jwtService = jwtService;
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
        String requestPath = request.getRequestURI();
        
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String ipAddress = SecurityEventLogger.getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            securityEventLogger.logTokenRevoked(ipAddress, endpoint);
            sendErrorResponse(response, "Token has been revoked", HttpStatus.UNAUTHORIZED);
            return;
        }
        
        try {
            Claims claims = jwtService.extractAllClaims(token);
            String username = claims.getSubject();
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtService.isTokenValid(token, username)) {
                    Long userId = claims.get("userId", Long.class);
                    String role = claims.get("role", String.class);
                    String fullName = claims.get("fullName", String.class);
                    
                    UserPrincipal userPrincipal = UserPrincipal.builder()
                        .id(userId)
                        .email(username)
                        .fullName(fullName)
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role)))
                        .build();
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    securityEventLogger.logTokenValid(username, ipAddress, endpoint);
                }
            }
        } catch (ExpiredJwtException ex) {
            if (requestPath.equals("/api/v1/auth/logout")) {
                try {
                    Claims claims = ex.getClaims();
                    String username = claims.getSubject();
                    Long userId = claims.get("userId", Long.class);
                    String role = claims.get("role", String.class);
                    String fullName = claims.get("fullName", String.class);
                    
                    UserPrincipal userPrincipal = UserPrincipal.builder()
                        .id(userId)
                        .email(username)
                        .fullName(fullName)
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role)))
                        .build();
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    securityEventLogger.logTokenExpired(ipAddress, endpoint);
                    filterChain.doFilter(request, response);
                    return;
                } catch (Exception e) {
                    // Fall through to error
                }
            }
            
            securityEventLogger.logTokenExpired(ipAddress, endpoint);
            sendErrorResponse(response, "Token expired", HttpStatus.UNAUTHORIZED);
            return;
        } catch (SignatureException ex) {
            securityEventLogger.logTokenInvalid(ipAddress, endpoint, "Invalid signature");
            sendErrorResponse(response, "Invalid token signature", HttpStatus.UNAUTHORIZED);
            return;
        } catch (JwtException | IllegalArgumentException ex) {
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
    
    private boolean isPublicEndpoint(String requestPath) {
        if (requestPath.equals("/api/v1/auth/login") || 
            requestPath.equals("/api/v1/auth/refresh")) {
            return true;
        }
        
        if (requestPath.startsWith("/swagger-ui") || 
            requestPath.startsWith("/v3/api-docs") ||
            requestPath.startsWith("/graphiql")) {
            return true;
        }
        
        if (requestPath.startsWith("/actuator")) {
            return true;
        }
        
        return false;
    }
}
