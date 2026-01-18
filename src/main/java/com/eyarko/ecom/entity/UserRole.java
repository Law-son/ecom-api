package com.eyarko.ecom.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserRole {
    CUSTOMER,
    ADMIN;

    @JsonCreator
    public static UserRole fromString(String value) {
        if (value == null) {
            return null;
        }
        return UserRole.valueOf(value.trim().toUpperCase());
    }
}


