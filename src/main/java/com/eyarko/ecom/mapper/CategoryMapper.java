package com.eyarko.ecom.mapper;

import com.eyarko.ecom.dto.CategoryResponse;
import com.eyarko.ecom.entity.Category;

public final class CategoryMapper {
    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryResponse.builder()
            .id(category.getId())
            .name(category.getName())
            .createdAt(category.getCreatedAt())
            .build();
    }
}

