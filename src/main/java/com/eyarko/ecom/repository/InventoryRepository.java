package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Inventory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(Long productId);
}

