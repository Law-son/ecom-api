package com.eyarko.ecom.entity;

import java.math.BigDecimal;
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
public class OrderItem {
    private Long id;
    private Order order;
    private Product product;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal priceAtTime;
}


