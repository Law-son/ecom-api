package com.eyarko.ecom.repository;

import com.eyarko.ecom.document.Review;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByProductId(Long productId);
    List<Review> findByUserId(Long userId);
    Page<Review> findByProductId(Long productId, Pageable pageable);
    Page<Review> findByUserId(Long userId, Pageable pageable);

    @Query("{}")
    Page<Review> findAllReviews(Pageable pageable);
}


