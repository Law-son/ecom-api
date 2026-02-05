package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Cart;
import com.eyarko.ecom.entity.CartItem;
import com.eyarko.ecom.entity.Product;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for cart persistence.
 */
@Repository
public class CartRepository {
    private static final String CART_SELECT =
        "SELECT cart_id, user_id, created_at, updated_at FROM carts";

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final RowMapper<Cart> cartRowMapper = (rs, rowNum) -> Cart.builder()
        .id(rs.getLong("cart_id"))
        .userId(rs.getLong("user_id"))
        .createdAt(toInstant(rs.getTimestamp("created_at")))
        .updatedAt(toInstant(rs.getTimestamp("updated_at")))
        .items(new ArrayList<>())
        .build();

    public CartRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public Optional<Cart> findByUserId(Long userId) {
        List<Cart> carts = jdbcTemplate.query(
            CART_SELECT + " WHERE user_id = ?",
            cartRowMapper,
            userId
        );
        if (carts.isEmpty()) {
            return Optional.empty();
        }
        attachItems(carts);
        return Optional.of(carts.get(0));
    }

    public Optional<Cart> findById(Long cartId) {
        List<Cart> carts = jdbcTemplate.query(
            CART_SELECT + " WHERE cart_id = ?",
            cartRowMapper,
            cartId
        );
        if (carts.isEmpty()) {
            return Optional.empty();
        }
        attachItems(carts);
        return Optional.of(carts.get(0));
    }

    public Cart createCart(Long userId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            String sql = "INSERT INTO carts (user_id) VALUES (?)";
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            return ps;
        }, keyHolder);
        Long key = extractKey(keyHolder, "cart_id");
        if (key == null) {
            return Cart.builder().userId(userId).items(new ArrayList<>()).build();
        }
        return findById(key).orElse(Cart.builder().id(key).userId(userId).items(new ArrayList<>()).build());
    }

    public Optional<CartItem> findItem(Long cartId, Long productId) {
        String sql = "SELECT cart_item_id, cart_id, product_id, quantity, unit_price, created_at, updated_at "
            + "FROM cart_items WHERE cart_id = ? AND product_id = ?";
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> CartItem.builder()
                .id(rs.getLong("cart_item_id"))
                .cartId(rs.getLong("cart_id"))
                .product(Product.builder().id(rs.getLong("product_id")).build())
                .quantity(rs.getInt("quantity"))
                .unitPrice(rs.getBigDecimal("unit_price"))
                .createdAt(toInstant(rs.getTimestamp("created_at")))
                .updatedAt(toInstant(rs.getTimestamp("updated_at")))
                .build(),
            cartId,
            productId
        ).stream().findFirst();
    }

    public void insertItem(Long cartId, Long productId, int quantity, BigDecimal unitPrice) {
        jdbcTemplate.update(
            "INSERT INTO cart_items (cart_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)",
            cartId,
            productId,
            quantity,
            unitPrice
        );
    }

    public void updateItemQuantity(Long cartId, Long productId, int quantity) {
        jdbcTemplate.update(
            "UPDATE cart_items SET quantity = ?, updated_at = CURRENT_TIMESTAMP WHERE cart_id = ? AND product_id = ?",
            quantity,
            cartId,
            productId
        );
    }

    public void deleteItem(Long cartId, Long productId) {
        jdbcTemplate.update(
            "DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?",
            cartId,
            productId
        );
    }

    public void clearCart(Long cartId) {
        jdbcTemplate.update("DELETE FROM cart_items WHERE cart_id = ?", cartId);
    }

    public void touchCart(Long cartId) {
        jdbcTemplate.update("UPDATE carts SET updated_at = CURRENT_TIMESTAMP WHERE cart_id = ?", cartId);
    }

    private void attachItems(List<Cart> carts) {
        if (carts == null || carts.isEmpty()) {
            return;
        }
        Map<Long, Cart> byId = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        for (Cart cart : carts) {
            byId.put(cart.getId(), cart);
            ids.add(cart.getId());
            if (cart.getItems() == null) {
                cart.setItems(new ArrayList<>());
            } else {
                cart.getItems().clear();
            }
        }
        String sql =
            "SELECT ci.cart_item_id, ci.cart_id, ci.product_id, ci.quantity, ci.unit_price, "
                + "ci.created_at, ci.updated_at, p.name AS product_name, p.image_url, p.price AS product_price "
                + "FROM cart_items ci JOIN products p ON p.product_id = ci.product_id "
                + "WHERE ci.cart_id IN (:ids) ORDER BY ci.cart_item_id ASC";
        namedJdbcTemplate.query(
            sql,
            new MapSqlParameterSource("ids", ids),
            (rs) -> {
                Long cartId = rs.getLong("cart_id");
                Cart cart = byId.get(cartId);
                if (cart == null) {
                    return;
                }
                Product product = Product.builder()
                    .id(rs.getLong("product_id"))
                    .name(rs.getString("product_name"))
                    .imageUrl(rs.getString("image_url"))
                    .price(rs.getBigDecimal("product_price"))
                    .build();
                CartItem item = CartItem.builder()
                    .id(rs.getLong("cart_item_id"))
                    .cartId(cartId)
                    .product(product)
                    .quantity(rs.getInt("quantity"))
                    .unitPrice(rs.getBigDecimal("unit_price"))
                    .createdAt(toInstant(rs.getTimestamp("created_at")))
                    .updatedAt(toInstant(rs.getTimestamp("updated_at")))
                    .build();
                cart.getItems().add(item);
            }
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
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
}

