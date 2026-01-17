package com.eyarko.ecom.mapper;

import com.eyarko.ecom.document.Review;
import com.eyarko.ecom.dto.ReviewResponse;

public final class ReviewMapper {
    private ReviewMapper() {
    }

    public static ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        return ReviewResponse.builder()
            .id(review.getId())
            .userId(review.getUserId())
            .productId(review.getProductId())
            .rating(review.getRating())
            .comment(review.getComment())
            .createdAt(review.getCreatedAt())
            .metadata(review.getMetadata())
            .build();
    }
}

