package com.eyarko.ecom.config;

import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.repository.InventoryRepository;
import com.eyarko.ecom.repository.ProductRepository;
import com.eyarko.ecom.util.InventoryStatusDisplay;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Optional dev-only inventory seeder for profiling and load tests.
 *
 * <p>Enabled only when:
 * <ul>
 *   <li>profile = dev</li>
 *   <li>app.profiling.seed-inventory.enabled=true</li>
 * </ul>
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.profiling.seed-inventory.enabled", havingValue = "true")
public class DevInventorySeedInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DevInventorySeedInitializer.class);
    private static final int DEFAULT_SEED_QUANTITY = 40;

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public DevInventorySeedInitializer(
        InventoryRepository inventoryRepository,
        ProductRepository productRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @PostConstruct
    @Transactional
    public void seedInventoryForProfiling() {
        List<Product> products = productRepository.findAll();
        final int[] created = {0};
        final int[] updated = {0};
        for (Product product : products) {
            Inventory inventory = inventoryRepository.findByProduct_Id(product.getId())
                .orElseGet(() -> {
                    Inventory createdInventory = Inventory.builder()
                        .product(product)
                        .quantity(DEFAULT_SEED_QUANTITY)
                        .statusDisplay(InventoryStatusDisplay.fromQuantity(DEFAULT_SEED_QUANTITY))
                        .build();
                    created[0]++;
                    return createdInventory;
                });
            Integer quantity = inventory.getQuantity();
            if (quantity == null || quantity <= 0) {
                inventory.setQuantity(DEFAULT_SEED_QUANTITY);
                inventory.setStatusDisplay(InventoryStatusDisplay.fromQuantity(DEFAULT_SEED_QUANTITY));
                updated[0]++;
            }
            inventoryRepository.save(inventory);
        }

        inventoryRepository.flush();
        logger.info("Dev inventory seed completed: {} inventory rows created, {} rows updated.", created[0], updated[0]);
    }
}
