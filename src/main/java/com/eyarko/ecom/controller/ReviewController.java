package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.dto.ReviewCreateRequest;
import com.eyarko.ecom.dto.ReviewResponse;
import com.eyarko.ecom.service.ReviewService;
import com.eyarko.ecom.util.ResponseUtil;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ApiResponse<ReviewResponse> createReview(@RequestBody ReviewCreateRequest request) {
        return ResponseUtil.success("Review created", reviewService.createReview(request));
    }

    @GetMapping
    public ApiResponse<List<ReviewResponse>> listReviews(
        @RequestParam(required = false) Long productId,
        @RequestParam(required = false) Long userId
    ) {
        return ResponseUtil.success("Reviews retrieved", reviewService.listReviews(productId, userId));
    }
}

