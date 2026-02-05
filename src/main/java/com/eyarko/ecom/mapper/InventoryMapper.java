package com.eyarko.ecom.mapper;

import com.eyarko.ecom.dto.InventoryResponse;
import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.util.InventoryStatusUtil;

public final class InventoryMapper {
    private InventoryMapper() {
    }

    public static InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        Product product = inventory.getProduct();
        String status = inventory.getStatus() != null
            ? inventory.getStatus()
            : InventoryStatusUtil.resolveStatus(inventory.getQuantity());
        return InventoryResponse.builder()
            .productId(product != null ? product.getId() : null)
            .quantity(inventory.getQuantity())
            .status(status)
            .lastUpdated(inventory.getLastUpdated())
            .build();
    }
}


