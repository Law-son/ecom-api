package com.eyarko.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Authentication response containing access and refresh tokens.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    /**
     * Short-lived JWT access token (default: 60 minutes).
     * Used for API authentication.
     */
    private String accessToken;

    /**
     * Long-lived refresh token (default: 7 days).
     * Used to obtain new access tokens without re-authentication.
     */
    private String refreshToken;

    /**
     * Token type (always "Bearer" for JWT).
     */
    @Builder.Default
    private String tokenType = "Bearer";
}

