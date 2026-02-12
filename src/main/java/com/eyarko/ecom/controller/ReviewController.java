package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.dto.ReviewCreateRequest;
import com.eyarko.ecom.dto.ReviewResponse;
import com.eyarko.ecom.service.ReviewService;
import com.eyarko.ecom.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Review endpoints for products.
 */
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Creates a new review.
     *
     * @param request review payload
     * @return created review
     */
    @PostMapping
    public ApiResponse<ReviewResponse> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        return ResponseUtil.success("Review created", reviewService.createReview(request));
    }

    /**
     * Lists reviews, optionally filtered by product or user.
     *
     * @param productId optional product id
     * @param userId optional user id
     * @return list of reviews
     */
    @GetMapping
    public ApiResponse<List<ReviewResponse>> listReviews(
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) Long userId
    ) {
        return ResponseUtil.success("Reviews retrieved", reviewService.listReviews(productId, userId));
    }
}

