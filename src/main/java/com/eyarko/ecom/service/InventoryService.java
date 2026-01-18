package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.InventoryAdjustRequest;
import com.eyarko.ecom.dto.InventoryResponse;
import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.mapper.InventoryMapper;
import com.eyarko.ecom.repository.InventoryRepository;
import com.eyarko.ecom.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    public InventoryResponse adjustInventory(InventoryAdjustRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        Inventory inventory = inventoryRepository.findByProductId(product.getId())
            .orElseGet(() -> Inventory.builder().product(product).build());
        inventory.setQuantity(request.getQuantity());
        return InventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    public InventoryResponse getInventoryByProduct(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found"));
        return InventoryMapper.toResponse(inventory);
    }
}


