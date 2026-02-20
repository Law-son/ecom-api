package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.dto.LoginRequest;
import com.eyarko.ecom.service.AuthService;
import com.eyarko.ecom.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 * <p>
 * Provides JWT-based authentication for the API.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user using email/password credentials and issues a signed JWT token.
     * <p>
     * The returned JWT token contains:
     * <ul>
     *   <li>Subject (sub): User email</li>
     *   <li>Issued At (iat): Token creation timestamp</li>
     *   <li>Expiration (exp): Token expiration timestamp</li>
     *   <li>Custom claims: userId, role, fullName, lastLogin</li>
     * </ul>
     * <p>
     * Token is signed using HMAC SHA-256 and should be sent in the Authorization header
     * as "Bearer &lt;token&gt;" for protected endpoints.
     *
     * @param request login request payload containing email and password
     * @return success response with JWT token string in data field
     */
    @PostMapping("/login")
    public ApiResponse<String> login(@Valid @RequestBody LoginRequest request) {
        return ResponseUtil.success("Login successful", authService.login(request));
    }
}

