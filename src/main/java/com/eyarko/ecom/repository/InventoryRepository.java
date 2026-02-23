package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Inventory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for inventory.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProduct_Id(Long productId);

    @Query(
        value = "SELECT product_id AS productId, quantity AS quantity FROM inventory WHERE product_id IN (:productIds)",
        nativeQuery = true
    )
    List<InventoryQuantityView> findQuantitiesByProductIds(@Param("productIds") List<Long> productIds);
}


