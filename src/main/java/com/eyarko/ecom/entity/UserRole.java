package com.eyarko.ecom.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Allowed roles for SQL users.
 */
public enum UserRole {
    CUSTOMER,
    STAFF,
    ADMIN;

    @JsonCreator
    public static UserRole fromString(String value) {
        if (value == null) {
            return null;
        }
        return UserRole.valueOf(value.trim().toUpperCase());
    }
}


