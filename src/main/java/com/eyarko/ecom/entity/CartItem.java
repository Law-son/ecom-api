package com.eyarko.ecom.entity;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SQL-backed cart item model.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    private Long id;
    private Long cartId;
    private Product product;
    private int quantity;
    private BigDecimal unitPrice;
    private Instant createdAt;
    private Instant updatedAt;
}

