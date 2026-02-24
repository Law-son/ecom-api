package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.AuthResponse;
import com.eyarko.ecom.dto.LoginRequest;
import com.eyarko.ecom.entity.RefreshToken;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.repository.UserRepository;
import com.eyarko.ecom.security.JwtService;
import com.eyarko.ecom.security.SecurityEventLogger;
import com.eyarko.ecom.security.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecurityEventLogger securityEventLogger;
    private final CacheManager cacheManager;

    public AuthService(
        UserRepository userRepository,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        RefreshTokenService refreshTokenService,
        TokenBlacklistService tokenBlacklistService,
        SecurityEventLogger securityEventLogger,
        CacheManager cacheManager
    ) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.securityEventLogger = securityEventLogger;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = SecurityEventLogger.getClientIpAddress(httpRequest);
        String userAgent = SecurityEventLogger.getUserAgent(httpRequest);
        
        try {
            // Use Spring Security's AuthenticationManager for authentication
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            // Get authenticated user
            User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

            // Update last login
            user.setLastLogin(Instant.now());
            userRepository.save(user);
            evictUserCache(user.getId());

            // Generate tokens
            String accessToken = jwtService.generateToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            // Log successful authentication
            securityEventLogger.logAuthenticationSuccess(user.getEmail(), ipAddress, userAgent);

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .build();
        } catch (BadCredentialsException ex) {
            securityEventLogger.logAuthenticationFailure(
                request.getEmail(), ipAddress, userAgent, "Invalid credentials"
            );
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenString);
        User user = refreshToken.getUser();

        // Generate new access token
        String accessToken = jwtService.generateToken(user);

        // Generate new refresh token (rotate refresh token)
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(newRefreshToken.getToken())
            .tokenType("Bearer")
            .build();
    }

    @Transactional
    public void logout(String email, String accessToken, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        refreshTokenService.revokeAllUserTokens(user.getId());
        
        // Blacklist the current access token if provided
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                Instant expiration = jwtService.extractExpiry(accessToken);
                tokenBlacklistService.blacklistToken(accessToken, expiration);
            } catch (Exception ex) {
                // If token is invalid, ignore (it's already unusable)
            }
        }
        
        // Log logout event
        String ipAddress = SecurityEventLogger.getClientIpAddress(httpRequest);
        securityEventLogger.logLogout(email, ipAddress);
        
        evictUserCache(user.getId());
    }
    
    @Transactional
    public void logout(String email, HttpServletRequest httpRequest) {
        logout(email, null, httpRequest);
    }
    

    private void evictUserCache(Long userId) {
        Cache cache = cacheManager.getCache("userById");
        if (cache != null && userId != null) {
            cache.evict(userId);
        }
    }
}

