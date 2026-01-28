package com.eyarko.ecom.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/**
 * SQL-backed user model.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;
    private String fullName;
    private String email;
    private String passwordHash;
    private UserRole role;
    private Instant createdAt;
    private Instant lastLogin;
}


