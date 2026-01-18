package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Override
    @EntityGraph(attributePaths = { "items", "items.product", "user" })
    Optional<Order> findById(Long id);

    @Override
    @EntityGraph(attributePaths = { "items", "items.product", "user" })
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = { "items", "items.product", "user" })
    Page<Order> findByUserId(Long userId, Pageable pageable);
}


