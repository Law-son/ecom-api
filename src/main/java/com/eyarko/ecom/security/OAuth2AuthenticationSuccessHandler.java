package com.eyarko.ecom.security;

import com.eyarko.ecom.entity.RefreshToken;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.repository.UserRepository;
import com.eyarko.ecom.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    ) throws IOException {
        UserPrincipalOAuth2User oauth2User = (UserPrincipalOAuth2User) authentication.getPrincipal();
        UserPrincipal userPrincipal = oauth2User.getPrincipal();
        
        User user = userRepository.findById(userPrincipal.getId())
            .orElseThrow(() -> new IllegalStateException("User not found: " + userPrincipal.getEmail()));

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        String url = redirectUri
            + "?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
            + "&refreshToken=" + URLEncoder.encode(refreshToken.getToken(), StandardCharsets.UTF_8)
            + "&tokenType=Bearer";

        response.sendRedirect(url);
    }
}
