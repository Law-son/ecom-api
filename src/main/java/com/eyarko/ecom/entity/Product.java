package com.eyarko.ecom.entity;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SQL-backed product model.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    private Long id;
    private Category category;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Instant createdAt;
    private BigDecimal avgRating;
    private Integer reviewCount;
}


