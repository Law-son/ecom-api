package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}

