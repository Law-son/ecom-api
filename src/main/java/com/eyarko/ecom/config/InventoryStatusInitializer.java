package com.eyarko.ecom.config;

import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.repository.InventoryRepository;
import com.eyarko.ecom.util.InventoryStatusDisplay;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Initializes inventory_status for existing inventory records on application startup.
 * <p>
 * This component ensures that all existing inventory records have a valid inventory_status
 * value, which is necessary when adding the column to an existing database.
 */
@Component
public class InventoryStatusInitializer {
    private static final Logger logger = LoggerFactory.getLogger(InventoryStatusInitializer.class);

    private final InventoryRepository inventoryRepository;

    @Autowired
    public InventoryStatusInitializer(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @PostConstruct
    @Transactional
    public void initializeInventoryStatus() {
        try {
            List<Inventory> inventories = inventoryRepository.findAll();
            int updated = 0;
            
            for (Inventory inv : inventories) {
                if (inv.getStatusDisplay() == null || inv.getStatusDisplay().isEmpty()) {
                    // Set statusDisplay based on quantity (same logic as @PreUpdate)
                    if (inv.getQuantity() != null) {
                        inv.setStatusDisplay(InventoryStatusDisplay.fromQuantity(inv.getQuantity()));
                        updated++;
                    } else {
                        inv.setStatusDisplay("Out of stock");
                        updated++;
                    }
                }
            }

            if (updated > 0) {
                inventoryRepository.saveAll(inventories);
                inventoryRepository.flush();
                logger.info("Initialized inventory_status for {} inventory records", updated);
            }
        } catch (Exception e) {
            logger.warn("Failed to initialize inventory_status: {}", e.getMessage());
            // Don't fail startup if this fails - it's a data migration issue
        }
    }
}

