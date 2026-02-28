package com.eyarko.ecom.service;

import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.entity.OrderItem;
import com.eyarko.ecom.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncOrderService {
    
    private static final Logger log = LoggerFactory.getLogger(AsyncOrderService.class);
    
    private final InventoryRepository inventoryRepository;
    private final CacheManager cacheManager;

    public AsyncOrderService(InventoryRepository inventoryRepository, CacheManager cacheManager) {
        this.inventoryRepository = inventoryRepository;
        this.cacheManager = cacheManager;
    }

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> processInventoryReservation(List<OrderItem> items) {
        try {
            items.forEach(this::reserveInventory);
            log.info("Inventory reservation completed for {} items", items.size());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Error reserving inventory", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> processInventoryRestoration(List<OrderItem> items) {
        try {
            items.stream()
                .filter(item -> item != null && item.getProduct() != null)
                .forEach(this::restoreInventory);
            log.info("Inventory restoration completed for {} items", items.size());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Error restoring inventory", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> evictProductCachesAsync(List<OrderItem> items) {
        try {
            Cache byId = cacheManager.getCache("productById");
            Cache lists = cacheManager.getCache("productLists");
            
            if (items != null && byId != null) {
                items.parallelStream()
                    .map(item -> item.getProduct() != null ? item.getProduct().getId() : null)
                    .filter(id -> id != null)
                    .forEach(byId::evict);
            }
            
            if (lists != null) {
                lists.clear();
            }
            
            log.info("Cache eviction completed for {} items", items != null ? items.size() : 0);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Error evicting caches", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void reserveInventory(OrderItem item) {
        Inventory inventory = inventoryRepository.findByProduct_Id(item.getProduct().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory not found"));
        
        int remaining = inventory.getQuantity() - item.getQuantity();
        if (remaining < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock");
        }
        
        inventory.setQuantity(remaining);
        inventoryRepository.save(inventory);
    }

    private void restoreInventory(OrderItem item) {
        Inventory inventory = inventoryRepository.findByProduct_Id(item.getProduct().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory not found"));
        
        inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
        inventoryRepository.save(inventory);
    }
}
