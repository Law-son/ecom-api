package com.eyarko.ecom.dto;

import com.eyarko.ecom.entity.UserRole;
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
public class UserCreateRequest {
    private String fullName;
    private String email;
    private String password;
    private UserRole role;
}

