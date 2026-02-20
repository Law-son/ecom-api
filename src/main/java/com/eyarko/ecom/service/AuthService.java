package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.AuthResponse;
import com.eyarko.ecom.dto.LoginRequest;
import com.eyarko.ecom.entity.RefreshToken;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.repository.UserRepository;
import com.eyarko.ecom.security.JwtService;
import java.time.Instant;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles authentication workflows including login, token refresh, and logout.
 */
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CacheManager cacheManager;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        RefreshTokenService refreshTokenService,
        CacheManager cacheManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.cacheManager = cacheManager;
    }

    /**
     * Authenticates a user by email and password.
     * <p>
     * Returns both access token (short-lived) and refresh token (long-lived).
     *
     * @param request login payload
     * @return authentication response with access and refresh tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        user.setLastLogin(Instant.now());
        userRepository.save(user);
        evictUserCache(user.getId());

        // Generate access token
        String accessToken = jwtService.generateToken(user);

        // Generate refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken.getToken())
            .tokenType("Bearer")
            .build();
    }

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * @param refreshTokenString refresh token string
     * @return new authentication response with new access and refresh tokens
     */
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

    /**
     * Logs out a user by revoking all refresh tokens.
     *
     * @param email user email
     */
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        refreshTokenService.revokeAllUserTokens(user.getId());
        evictUserCache(user.getId());
    }

    private void evictUserCache(Long userId) {
        Cache cache = cacheManager.getCache("userById");
        if (cache != null && userId != null) {
            cache.evict(userId);
        }
    }
}

