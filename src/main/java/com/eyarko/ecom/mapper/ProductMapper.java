package com.eyarko.ecom.mapper;

import com.eyarko.ecom.dto.ProductResponse;
import com.eyarko.ecom.entity.Category;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.util.InventoryStatusDisplay;

public final class ProductMapper {
    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        return toResponse(product, null);
    }

    public static ProductResponse toResponse(Product product, Integer stockQuantity) {
        if (product == null) {
            return null;
        }
        Category category = product.getCategory();
        int qty = stockQuantity != null ? stockQuantity : 0;
        boolean inStock = qty > 0;
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
            .stockQuantity(stockQuantity)
            .inStock(inStock)
            .stockStatus(InventoryStatusDisplay.fromQuantity(qty))
            .build();
    }
}


