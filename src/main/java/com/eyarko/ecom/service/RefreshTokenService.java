package com.eyarko.ecom.service;

import com.eyarko.ecom.entity.RefreshToken;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing refresh tokens.
 * <p>
 * Refresh tokens are long-lived tokens stored in the database that allow
 * users to obtain new access tokens without re-authenticating.
 */
@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationDays;

    public RefreshTokenService(
        RefreshTokenRepository refreshTokenRepository,
        @Value("${app.security.jwt.refresh-expiration-days:7}") long refreshTokenExpirationDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    /**
     * Creates a new refresh token for a user.
     *
     * @param user user entity
     * @return refresh token entity
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke existing tokens for the user
        refreshTokenRepository.revokeAllByUserId(user.getId());

        // Generate a secure random token
        String token = UUID.randomUUID().toString();

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(refreshTokenExpirationDays * 24 * 60 * 60);

        RefreshToken refreshToken = RefreshToken.builder()
            .token(token)
            .user(user)
            .expiresAt(expiresAt)
            .createdAt(now)
            .revoked(false)
            .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validates a refresh token.
     *
     * @param token refresh token string
     * @return refresh token entity if valid
     * @throws RuntimeException if token is invalid, expired, or revoked
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token has expired");
        }

        return refreshToken;
    }

    /**
     * Revokes a refresh token.
     *
     * @param token refresh token string
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
            .ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
    }

    /**
     * Revokes all refresh tokens for a user (logout).
     *
     * @param userId user ID
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * Deletes expired refresh tokens (cleanup task).
     */
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
    }
}

