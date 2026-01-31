package com.eyarko.ecom.dto;

import com.eyarko.ecom.entity.UserRole;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Long id;
    private String fullName;
    private String email;
    private UserRole role;
    private Instant lastLogin;
    private String accessToken;
    private String tokenType;
    private Instant expiresAt;
}

