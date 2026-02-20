package com.eyarko.ecom.security;

import com.eyarko.ecom.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.stereotype.Service;

/**
 * JWT token generation and validation service.
 * <p>
 * This service handles:
 * <ul>
 *   <li>Token generation with required claims (subject, issued time, expiration)</li>
 *   <li>Token validation using HMAC SHA-256 signature verification</li>
 *   <li>Rejection of tampered or expired tokens</li>
 * </ul>
 * <p>
 * Token Claims:
 * <ul>
 *   <li><b>sub</b> (subject): User email address</li>
 *   <li><b>iat</b> (issued at): Token creation timestamp</li>
 *   <li><b>exp</b> (expiration): Token expiration timestamp</li>
 *   <li><b>userId</b>: User ID</li>
 *   <li><b>role</b>: User role (CUSTOMER, ADMIN)</li>
 *   <li><b>fullName</b>: User's full name</li>
 *   <li><b>lastLogin</b>: Last login timestamp</li>
 * </ul>
 * <p>
 * Signature Algorithm: HMAC SHA-256 (HS256)
 * <p>
 * To decode tokens for testing:
 * <ul>
 *   <li>Use jwt.io or Postman's JWT decoder</li>
 *   <li>Copy the token from login response</li>
 *   <li>Paste into decoder to view claims</li>
 * </ul>
 */
@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(JwtProperties properties) {
        String secret = properties.getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = properties.getExpirationMinutes();
    }

    /**
     * Generates a signed JWT token for a user.
     * <p>
     * Includes required claims:
     * <ul>
     *   <li>Subject (sub): User email</li>
     *   <li>Issued At (iat): Current timestamp</li>
     *   <li>Expiration (exp): Current time + expiration minutes</li>
     * </ul>
     * <p>
     * Also includes custom claims: userId, role, fullName, lastLogin
     * <p>
     * Token is signed using HMAC SHA-256 algorithm.
     *
     * @param user User entity to generate token for
     * @return Signed JWT token string
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
            .subject(user.getEmail())  // Required: subject claim
            .issuedAt(Date.from(now))   // Required: issued time claim
            .expiration(Date.from(expiry)) // Required: expiration claim
            .claim("userId", user.getId())
            .claim("role", user.getRole().name())
            .claim("fullName", user.getFullName())
            .claim("lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : null)
            .signWith(signingKey, Jwts.SIG.HS256) // HMAC SHA-256 signature
            .compact();
    }

    public Instant extractExpiry(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validates a JWT token.
     * <p>
     * Checks:
     * <ul>
     *   <li>Token signature is valid (not tampered)</li>
     *   <li>Token is not expired</li>
     *   <li>Subject matches expected username</li>
     * </ul>
     *
     * @param token JWT token to validate
     * @param expectedUsername Expected username (email) from token subject
     * @return true if token is valid, false otherwise
     * @throws JwtException if token is tampered or expired
     */
    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            String username = extractUsername(token);
            return username != null && username.equals(expectedUsername) && !isTokenExpired(token);
        } catch (JwtException ex) {
            return false;
        }
    }

    /**
     * Checks if a token is expired.
     *
     * @param token JWT token to check
     * @return true if token is expired, false otherwise
     * @throws JwtException if token is invalid or tampered
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            return expiration != null && expiration.before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (JwtException ex) {
            throw ex;
        }
    }

    /**
     * Parses and validates JWT token claims.
     * <p>
     * Uses HMAC SHA-256 signature verification to detect tampering.
     * Throws JwtException if:
     * <ul>
     *   <li>Token signature is invalid (tampered)</li>
     *   <li>Token format is invalid</li>
     *   <li>Token is expired (ExpiredJwtException)</li>
     * </ul>
     *
     * @param token JWT token to parse
     * @return Token claims
     * @throws JwtException if token is invalid, tampered, or expired
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(signingKey) // HMAC SHA-256 signature verification
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException ex) {
            throw ex; // Re-throw expired token exception
        } catch (JwtException ex) {
            // Token is tampered or invalid
            throw ex;
        }
    }
}

