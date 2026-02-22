package com.eyarko.ecom.security;

import com.eyarko.ecom.entity.RefreshToken;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.repository.UserRepository;
import com.eyarko.ecom.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * On successful OAuth2 login, issues application tokens and redirects to the frontend.
 * <p>
 * Redirect format:
 * {@code {redirectUri}?accessToken=...&refreshToken=...&tokenType=Bearer}
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final String redirectUri;

    public OAuth2AuthenticationSuccessHandler(
        JwtService jwtService,
        RefreshTokenService refreshTokenService,
        UserRepository userRepository,
        @Value("${app.security.oauth2.redirect-uri:http://localhost:5173/oauth2/redirect}") String redirectUri
    ) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.redirectUri = redirectUri;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        User user;
        
        // Handle different OAuth2 principal types (OidcUser or OAuth2User)
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserPrincipalOAuth2User) {
            // Our custom wrapper (from CustomOAuth2UserService)
            UserPrincipalOAuth2User oauth2User = (UserPrincipalOAuth2User) principal;
            UserPrincipal userPrincipal = oauth2User.getPrincipal();
            Long userId = userPrincipal.getId();
            user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                    "OAuth2 user not found after login: userId=" + userId + ", email=" + userPrincipal.getEmail()));
        } else if (principal instanceof OidcUser) {
            // OIDC user (when using "openid" scope)
            OidcUser oidcUser = (OidcUser) principal;
            String email = oidcUser.getEmail();
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("OIDC user has no email");
            }
            user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException(
                    "OAuth2 user not found after login: email=" + email));
        } else if (principal instanceof OAuth2User) {
            // Standard OAuth2 user
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = oauth2User.getAttribute("email");
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("OAuth2 user has no email");
            }
            user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException(
                    "OAuth2 user not found after login: email=" + email));
        } else {
            throw new IllegalStateException("Unsupported OAuth2 principal type: " + principal.getClass().getName());
        }

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        String url = redirectUri
            + "?accessToken=" + enc(accessToken)
            + "&refreshToken=" + enc(refreshToken.getToken())
            + "&tokenType=Bearer";

        response.sendRedirect(url);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}


