package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for carts.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findById(Long id);
}

