package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for orders.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    java.util.Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    Page<Order> findByUser_Id(Long userId, Pageable pageable);
}

