package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.InventoryAdjustRequest;
import com.eyarko.ecom.dto.InventoryResponse;
import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.util.InventoryStatusDisplay;
import com.eyarko.ecom.mapper.InventoryMapper;
import com.eyarko.ecom.repository.InventoryRepository;
import com.eyarko.ecom.repository.ProductRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
    private final CacheManager cacheManager;
    private final InventoryLockManager inventoryLockManager;

    public InventoryService(
        InventoryRepository inventoryRepository,
        ProductRepository productRepository,
        CacheManager cacheManager,
        InventoryLockManager inventoryLockManager
    ) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.cacheManager = cacheManager;
        this.inventoryLockManager = inventoryLockManager;
    }

    /**
     * Adjusts inventory quantity for a product.
     *
     * @param request inventory adjustment payload
     * @return updated inventory
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public InventoryResponse adjustInventory(InventoryAdjustRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        Inventory saved = inventoryLockManager.withProductLock(product.getId(), () -> {
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(product.getId())
                .orElseGet(() -> Inventory.builder().product(product).quantity(0).statusDisplay("Out of stock").build());
            inventory.setQuantity(request.getQuantity());
            inventory.setStatusDisplay(InventoryStatusDisplay.fromQuantity(inventory.getQuantity()));
            return inventoryRepository.save(inventory);
        });
        evictProductCaches(product.getId());
        return InventoryMapper.toResponse(saved);
    }

    /**
     * Retrieves inventory for a product, returning zero quantity if missing.
     *
     * @param productId product id
     * @return inventory details
     */
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return inventoryRepository.findByProduct_Id(productId)
            .map(InventoryMapper::toResponse)
            .orElseGet(() -> InventoryResponse.builder()
                .productId(productId)
                .quantity(0)
                .stockStatus("Out of stock")
                .lastUpdated(null)
                .build());
    }

    private void evictProductCaches(Long productId) {
        Cache byId = cacheManager.getCache("productById");
        if (byId != null && productId != null) {
            byId.evict(productId);
        }
        Cache lists = cacheManager.getCache("productLists");
        if (lists != null) {
            lists.clear();
        }
    }
}


