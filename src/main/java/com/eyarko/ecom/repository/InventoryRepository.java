package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.entity.Product;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for inventory persistence.
 */
@Repository
public class InventoryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Inventory> rowMapper = (rs, rowNum) -> Inventory.builder()
        .id(rs.getLong("inventory_id"))
        .product(Product.builder().id(rs.getLong("product_id")).build())
        .quantity((Integer) rs.getObject("quantity"))
        .lastUpdated(toInstant(rs.getTimestamp("last_updated")))
        .build();

    public InventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds inventory by product id.
     *
     * @param productId product id
     * @return optional inventory
     */
    public Optional<Inventory> findByProductId(Long productId) {
        return jdbcTemplate.query(
            "SELECT inventory_id, product_id, quantity, last_updated FROM inventory WHERE product_id = ?",
            rowMapper,
            productId
        ).stream().findFirst();
    }

    /**
     * Finds quantity for each product id. Missing product ids get 0.
     *
     * @param productIds product ids
     * @return map of product id to quantity
     */
    public Map<Long, Integer> findQuantityByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = productIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT product_id, quantity FROM inventory WHERE product_id IN (" + placeholders + ")",
            productIds.toArray()
        );
        return rows.stream()
            .collect(Collectors.toMap(
                row -> ((Number) row.get("product_id")).longValue(),
                row -> ((Number) row.get("quantity")).intValue()
            ));
    }

    /**
     * Inserts or updates inventory.
     *
     * @param inventory inventory to save
     * @return saved inventory
     */
    public Inventory save(Inventory inventory) {
        if (inventory.getId() == null) {
            return insertInventory(inventory);
        }
        return updateInventory(inventory);
    }

    private static java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Long extractKey(KeyHolder keyHolder, String... names) {
        if (keyHolder.getKeys() != null) {
            Map<String, Object> keys = keyHolder.getKeys();
            for (String name : names) {
                Object value = keys.get(name);
                if (value == null) {
                    value = keys.get(name.toUpperCase());
                }
                if (value == null) {
                    value = keys.get(name.toLowerCase());
                }
                if (value instanceof Number number) {
                    return number.longValue();
                }
            }
            for (Object value : keys.values()) {
                if (value instanceof Number number) {
                    return number.longValue();
                }
            }
        }
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    private Inventory insertInventory(Inventory inventory) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO inventory (product_id, quantity, last_updated) VALUES (?, ?, NOW())",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, inventory.getProduct().getId());
            ps.setInt(2, inventory.getQuantity());
            return ps;
        }, keyHolder);
        Long key = extractKey(keyHolder, "inventory_id");
        if (key == null) {
            return inventory;
        }
        Inventory saved = findByProductId(inventory.getProduct().getId()).orElse(inventory);
        if (saved.getId() == null) {
            saved.setId(key);
        }
        return saved;
    }

    private Inventory updateInventory(Inventory inventory) {
        jdbcTemplate.update(
            "UPDATE inventory SET quantity = ?, last_updated = NOW() WHERE inventory_id = ?",
            inventory.getQuantity(),
            inventory.getId()
        );
        return findByProductId(inventory.getProduct().getId()).orElse(inventory);
    }
}


