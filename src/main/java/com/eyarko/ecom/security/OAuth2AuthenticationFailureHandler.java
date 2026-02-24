package com.eyarko.ecom.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final String redirectUri;

    public OAuth2AuthenticationFailureHandler(
        @Value("${app.security.oauth2.redirect-uri:http://localhost:5173/oauth2/redirect}") String redirectUri
    ) {
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        String errorMessage = exception.getMessage() != null 
            ? exception.getMessage() 
            : "OAuth2 authentication failed";
        
        String url = redirectUri + "?error=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        response.sendRedirect(url);
    }
}
