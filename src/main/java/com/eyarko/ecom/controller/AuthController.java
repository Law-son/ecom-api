package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.dto.AuthResponse;
import com.eyarko.ecom.dto.LoginRequest;
import com.eyarko.ecom.security.UserPrincipal;
import com.eyarko.ecom.service.AuthService;
import com.eyarko.ecom.util.ResponseUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        AuthResponse authResponse = authService.login(request, httpRequest);
        
        Cookie refreshTokenCookie = new Cookie("refreshToken", authResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
        httpResponse.addCookie(refreshTokenCookie);
        
        return ResponseUtil.success("Login successful", 
            AuthResponse.builder()
                .accessToken(authResponse.getAccessToken())
                .tokenType("Bearer")
                .build());
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse httpResponse
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token not found in cookies");
        }
        
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        
        Cookie refreshTokenCookie = new Cookie("refreshToken", authResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
        httpResponse.addCookie(refreshTokenCookie);
        
        return ResponseUtil.success("Token refreshed successfully", 
            AuthResponse.builder()
                .accessToken(authResponse.getAccessToken())
                .tokenType("Bearer")
                .build());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
        HttpServletRequest request,
        HttpServletResponse httpResponse
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String email = userPrincipal.getEmail();
            
            String authHeader = request.getHeader("Authorization");
            String accessToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            }
            
            authService.logout(email, accessToken, request);
        }
        
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        httpResponse.addCookie(refreshTokenCookie);
        
        return ResponseUtil.success("Logout successful", null);
    }
}
