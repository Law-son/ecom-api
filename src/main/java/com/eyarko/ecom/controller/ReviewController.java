package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.dto.PagedResponse;
import com.eyarko.ecom.dto.ReviewCreateRequest;
import com.eyarko.ecom.dto.ReviewResponse;
import com.eyarko.ecom.service.ReviewService;
import com.eyarko.ecom.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     * @param page page index
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDir sort direction
     * @return paged list of reviews
     */
    @GetMapping
    public ApiResponse<PagedResponse<ReviewResponse>> listReviews(
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseDirection(sortDir), sortBy));
        return ResponseUtil.success("Reviews retrieved", reviewService.listReviews(productId, userId, pageable));
    }

    private Sort.Direction parseDirection(String sortDir) {
        try {
            return Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException ex) {
            return Sort.Direction.ASC;
        }
    }
}

