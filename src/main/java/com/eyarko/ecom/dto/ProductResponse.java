package com.eyarko.ecom.dto;

import java.math.BigDecimal;
import java.time.Instant;
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
public class ProductResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private Instant createdAt;
    /** Available quantity from inventory; null if no inventory row. */
    private Integer stockQuantity;
    /** True when stockQuantity != null and stockQuantity > 0. */
    private Boolean inStock;
}


