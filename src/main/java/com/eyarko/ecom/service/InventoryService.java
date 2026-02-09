package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.InventoryAdjustRequest;
import com.eyarko.ecom.dto.InventoryResponse;
import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.mapper.InventoryMapper;
import com.eyarko.ecom.repository.InventoryRepository;
import com.eyarko.ecom.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Inventory business logic.
 */
@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * Adjusts inventory quantity for a product.
     *
     * @param request inventory adjustment payload
     * @return updated inventory
     */
    @CacheEvict(value = "products", allEntries = true)
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public InventoryResponse adjustInventory(InventoryAdjustRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
            .orElseGet(() -> Inventory.builder().product(product).build());
        inventory.setQuantity(request.getQuantity());
        return InventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    /**
     * Retrieves inventory for a product, returning zero quantity if missing.
     *
     * @param productId product id
     * @return inventory details
     */
    public InventoryResponse getInventoryByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return inventoryRepository.findByProduct_Id(productId)
            .map(InventoryMapper::toResponse)
            .orElseGet(() -> InventoryResponse.builder()
                .productId(productId)
                .quantity(0)
                .lastUpdated(null)
                .build());
    }
}


