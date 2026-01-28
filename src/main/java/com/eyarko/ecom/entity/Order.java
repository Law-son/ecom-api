package com.eyarko.ecom.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/**
 * SQL-backed order model.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    private Long id;
    private User user;
    private Instant orderDate;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItem> items = new ArrayList<>();
}


