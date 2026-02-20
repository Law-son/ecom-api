package com.eyarko.ecom.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for managing token blacklist using in-memory hash map.
 * <p>
 * This service provides:
 * <ul>
 *   <li>Token blacklisting using hashed token storage (SHA-256)</li>
 *   <li>In-memory hash map for fast O(1) lookup</li>
 *   <li>Automatic cleanup of expired blacklist entries</li>
 * </ul>
 * <p>
 * <b>Security Features:</b>
 * <ul>
 *   <li>Tokens are hashed (SHA-256) before storage to prevent token exposure</li>
 *   <li>ConcurrentHashMap ensures thread-safe operations</li>
 *   <li>Automatic expiration cleanup prevents memory leaks</li>
 * </ul>
 * <p>
 * <b>Use Cases:</b>
 * <ul>
 *   <li>Blacklist revoked access tokens (logout, token rotation)</li>
 *   <li>Prevent reuse of compromised tokens</li>
 *   <li>Support immediate token invalidation</li>
 * </ul>
 */
@Service
public class TokenBlacklistService {
    /**
     * In-memory hash map storing blacklisted tokens.
     * Key: SHA-256 hash of token, Value: Expiration timestamp
     */
    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();
    private final MessageDigest digest;

    public TokenBlacklistService() {
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    /**
     * Adds a token to the blacklist.
     * <p>
     * The token is hashed using SHA-256 before storage to prevent token exposure
     * in memory dumps or logs.
     *
     * @param token JWT token to blacklist
     * @param expirationTime Token expiration time (used for cleanup)
     */
    public void blacklistToken(String token, Instant expirationTime) {
        String tokenHash = hashToken(token);
        blacklist.put(tokenHash, expirationTime);
    }

    /**
     * Checks if a token is blacklisted.
     * <p>
     * Uses O(1) hash map lookup for fast validation.
     *
     * @param token JWT token to check
     * @return true if token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        String tokenHash = hashToken(token);
        Instant expiration = blacklist.get(tokenHash);
        
        if (expiration == null) {
            return false;
        }
        
        // If token has expired, remove it from blacklist
        if (expiration.isBefore(Instant.now())) {
            blacklist.remove(tokenHash);
            return false;
        }
        
        return true;
    }

    /**
     * Removes a token from the blacklist.
     *
     * @param token JWT token to remove from blacklist
     */
    public void removeFromBlacklist(String token) {
        String tokenHash = hashToken(token);
        blacklist.remove(tokenHash);
    }

    /**
     * Clears all expired entries from the blacklist.
     * <p>
     * This method is scheduled to run periodically to prevent memory leaks.
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

    /**
     * Gets the current size of the blacklist.
     * Useful for monitoring and debugging.
     *
     * @return number of blacklisted tokens
     */
    public int getBlacklistSize() {
        return blacklist.size();
    }

    /**
     * Hashes a token using SHA-256.
     * <p>
     * This ensures tokens are not stored in plain text, providing an additional
     * layer of security in case of memory dumps or logs.
     *
     * @param token JWT token to hash
     * @return SHA-256 hash of the token (hex string)
     */
    private String hashToken(String token) {
        synchronized (digest) {
            digest.reset();
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        }
    }

    /**
     * Converts byte array to hexadecimal string.
     *
     * @param bytes byte array
     * @return hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

