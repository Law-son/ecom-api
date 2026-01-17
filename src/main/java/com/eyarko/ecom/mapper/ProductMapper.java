package com.eyarko.ecom.mapper;

import com.eyarko.ecom.dto.ProductResponse;
import com.eyarko.ecom.entity.Category;
import com.eyarko.ecom.entity.Product;

public final class ProductMapper {
    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        Category category = product.getCategory();
        return ProductResponse.builder()
            .id(product.getId())
            .categoryId(category != null ? category.getId() : null)
            .categoryName(category != null ? category.getName() : null)
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice())
            .imageUrl(product.getImageUrl())
            .avgRating(product.getAvgRating())
            .reviewCount(product.getReviewCount())
            .createdAt(product.getCreatedAt())
            .build();
    }
}

