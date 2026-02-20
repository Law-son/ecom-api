package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.RefreshToken;
import com.eyarko.ecom.entity.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for refresh token operations.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    /**
     * Finds a refresh token by token string.
     *
     * @param token token string
     * @return optional refresh token
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Finds all valid (non-revoked, non-expired) refresh tokens for a user.
     *
     * @param user user entity
     * @param now current timestamp
     * @return optional refresh token
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiresAt > :now ORDER BY rt.createdAt DESC")
    Optional<RefreshToken> findValidTokenByUser(User user, Instant now);

    /**
     * Revokes all refresh tokens for a user.
     *
     * @param userId user ID
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllByUserId(Long userId);

    /**
     * Deletes expired refresh tokens.
     *
     * @param now current timestamp
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(Instant now);
}

