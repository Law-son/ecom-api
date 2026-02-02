package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.AuthResponse;
import com.eyarko.ecom.dto.LoginRequest;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.repository.UserRepository;
import com.eyarko.ecom.security.JwtService;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles authentication workflows.
 */
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Authenticates a user by email and password.
     *
     * @param request login payload
     * @return authentication response
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        user.setLastLogin(Instant.now());
        userRepository.save(user);
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .role(user.getRole())
            .lastLogin(user.getLastLogin())
            .accessToken(token)
            .tokenType("Bearer")
            .expiresAt(jwtService.extractExpiry(token))
            .build();
    }
}

