package com.eyarko.ecom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import com.eyarko.ecom.util.InventoryStatusDisplay;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SQL-backed inventory model.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Display string: "Out of stock", "N unit(s) in stock", "Few units in stock", "In stock". */
    @Column(name = "inventory_status", nullable = false, length = 50)
    private String statusDisplay;

    @Column(name = "last_updated", insertable = false, updatable = false)
    private Instant lastUpdated;

    /** Keeps inventory_status in sync with quantity on every persist/update (orders, adjust, etc.). */
    @PrePersist
    @PreUpdate
    void syncStatusDisplay() {
        if (quantity != null) {
            this.statusDisplay = InventoryStatusDisplay.fromQuantity(quantity);
        }
    }
}


