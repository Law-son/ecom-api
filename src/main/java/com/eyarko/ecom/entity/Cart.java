package com.eyarko.ecom.entity;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SQL-backed cart model.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {
    private Long id;
    private Long userId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CartItem> items;
}

