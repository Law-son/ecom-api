package com.eyarko.ecom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eyarko.ecom.document.Review;
import com.eyarko.ecom.dto.ReviewCreateRequest;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.repository.ProductRepository;
import com.eyarko.ecom.repository.ReviewRepository;
import com.eyarko.ecom.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void createReview_updatesProductRatingSummary() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
            .userId(1L)
            .productId(2L)
            .rating(5)
            .comment("Great product")
            .build();

        Product product = Product.builder().id(2L).build();
        when(userRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findById(2L)).thenReturn(Optional.of(product));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.findByProductId(2L)).thenReturn(
            List.of(
                Review.builder().rating(5).build(),
                Review.builder().rating(3).build()
            )
        );

        reviewService.createReview(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product savedProduct = captor.getValue();
        assertThat(savedProduct.getReviewCount()).isEqualTo(2);
        assertThat(savedProduct.getAvgRating()).isEqualTo(new BigDecimal("4.00"));
    }
}


