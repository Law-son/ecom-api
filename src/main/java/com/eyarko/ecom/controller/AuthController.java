package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.dto.AuthResponse;
import com.eyarko.ecom.dto.LoginRequest;
import com.eyarko.ecom.dto.RefreshTokenRequest;
import com.eyarko.ecom.security.UserPrincipal;
import com.eyarko.ecom.service.AuthService;
import com.eyarko.ecom.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 * <p>
 * Provides JWT-based authentication with refresh token support for the API.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user using email/password credentials and issues access and refresh tokens.
     * <p>
     * The returned access token (JWT) contains:
     * <ul>
     *   <li>Subject (sub): User email</li>
     *   <li>Issued At (iat): Token creation timestamp</li>
     *   <li>Expiration (exp): Token expiration timestamp</li>
     *   <li>Custom claims: userId, role, fullName, lastLogin</li>
     * </ul>
     * <p>
     * Access token is short-lived (default: 60 minutes) and should be sent in the Authorization header
     * as "Bearer &lt;token&gt;" for protected endpoints.
     * <p>
     * Refresh token is long-lived (default: 7 days) and should be stored securely by the client.
     * Use it to obtain new access tokens via the /refresh endpoint.
     *
     * @param request login request payload containing email and password
     * @return success response with AuthResponse containing accessToken and refreshToken
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseUtil.success("Login successful", authService.login(request));
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * <p>
     * This endpoint allows clients to obtain a new access token without re-authenticating.
     * The refresh token is rotated (old one is revoked, new one is issued) for security.
     *
     * @param request refresh token request containing the refresh token
     * @return success response with new AuthResponse containing new accessToken and refreshToken
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseUtil.success("Token refreshed successfully", authService.refreshToken(request.getRefreshToken()));
    }

    /**
     * Logs out the current user by revoking all refresh tokens.
     * <p>
     * Requires authentication. All refresh tokens for the authenticated user are revoked,
     * effectively logging them out from all devices.
     *
     * @return success response
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String email = userPrincipal.getEmail();
            authService.logout(email);
        }
        return ResponseUtil.success("Logout successful", null);
    }
}

