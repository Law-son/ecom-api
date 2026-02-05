package com.eyarko.ecom.mapper;

import com.eyarko.ecom.dto.InventoryResponse;
import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.entity.Product;

public final class InventoryMapper {
    private InventoryMapper() {
    }

    public static InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        Product product = inventory.getProduct();
        return InventoryResponse.builder()
            .productId(product != null ? product.getId() : null)
            .quantity(inventory.getQuantity())
            .status(inventory.getStatus())
            .lastUpdated(inventory.getLastUpdated())
            .build();
    }
}


