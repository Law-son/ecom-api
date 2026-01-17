package com.eyarko.ecom.mapper;

import com.eyarko.ecom.dto.UserResponse;
import com.eyarko.ecom.entity.User;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .lastLogin(user.getLastLogin())
            .build();
    }
}

