package com.eyarko.ecom.repository;

import com.eyarko.ecom.document.Review;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByProductId(Long productId);
    Page<Review> findByProductId(Long productId, Pageable pageable);
    Page<Review> findByUserId(Long userId, Pageable pageable);

    @Aggregation(pipeline = {
        "{ '$match': { 'productId': ?0 } }",
        "{ '$group': { '_id': '$productId', 'reviewCount': { '$sum': 1 }, 'avgRating': { '$avg': '$rating' } } }",
        "{ '$project': { '_id': 0, 'reviewCount': 1, 'avgRating': 1 } }"
    })
    Optional<ReviewStatsProjection> getReviewStatsByProductId(Long productId);

    @Query("{}")
    Page<Review> findAllReviews(Pageable pageable);

    interface ReviewStatsProjection {
        Integer getReviewCount();
        Double getAvgRating();
    }
}


