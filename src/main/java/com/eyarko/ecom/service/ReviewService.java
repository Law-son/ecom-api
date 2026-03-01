package com.eyarko.ecom.service;

import com.eyarko.ecom.document.Review;
import com.eyarko.ecom.dto.PagedResponse;
import com.eyarko.ecom.dto.ReviewCreateRequest;
import com.eyarko.ecom.dto.ReviewResponse;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.mapper.ReviewMapper;
import com.eyarko.ecom.repository.ProductRepository;
import com.eyarko.ecom.repository.ReviewRepository;
import com.eyarko.ecom.repository.UserRepository;
import io.micrometer.core.annotation.Timed;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Review business logic.
 */
@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ReviewService(
        ReviewRepository reviewRepository,
        UserRepository userRepository,
        ProductRepository productRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    /**
     * Creates a review and updates product rating stats.
     *
     * @param request review payload
     * @return created review
     */
    @Timed(value = "app.reviews.create.timed", description = "Time spent creating a review")
    public ReviewResponse createReview(ReviewCreateRequest request) {
        if (!userRepository.existsById(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        Review review = Review.builder()
            .userId(request.getUserId())
            .productId(request.getProductId())
            .rating(request.getRating())
            .comment(request.getComment())
            .metadata(request.getMetadata())
            .createdAt(Instant.now())
            .build();

        Review saved = reviewRepository.save(review);
        try {
            updateProductRating(product);
        } catch (RuntimeException ex) {
            reviewRepository.deleteById(saved.getId());
            throw ex;
        }
        return ReviewMapper.toResponse(saved);
    }

    /**
     * Lists reviews by product or user.
     *
     * @param productId optional product id
     * @param userId optional user id
     * @param pageable paging and sorting options
     * @return paged list of reviews
     */
    @Timed(value = "app.reviews.list.timed", description = "Time spent listing reviews")
    public PagedResponse<ReviewResponse> listReviews(Long productId, Long userId, Pageable pageable) {
        Page<Review> page;
        if (productId != null) {
            page = reviewRepository.findByProductId(productId, pageable);
        } else if (userId != null) {
            page = reviewRepository.findByUserId(userId, pageable);
        } else {
            page = reviewRepository.findAllReviews(pageable);
        }
        List<ReviewResponse> items = page.getContent().parallelStream()
            .map(ReviewMapper::toResponse)
            .collect(Collectors.toList());
        return PagedResponse.<ReviewResponse>builder()
            .items(items)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
    }

    private void updateProductRating(Product product) {
        ReviewRepository.ReviewStatsProjection stats = reviewRepository
            .getReviewStatsByProductId(product.getId())
            .orElse(null);
        int count = stats != null && stats.getReviewCount() != null ? stats.getReviewCount() : 0;
        BigDecimal avg = BigDecimal.ZERO;
        if (stats != null && stats.getAvgRating() != null) {
            avg = BigDecimal.valueOf(stats.getAvgRating()).setScale(2, RoundingMode.HALF_UP);
        }
        product.setAvgRating(avg);
        product.setReviewCount(count);
        productRepository.save(product);
    }
}


