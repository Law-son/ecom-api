package com.eyarko.ecom.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Allowed order status values.
 */
public enum OrderStatus {
    PENDING,
    RECEIVED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    @JsonCreator
    public static OrderStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        return OrderStatus.valueOf(value.trim().toUpperCase());
    }
}


