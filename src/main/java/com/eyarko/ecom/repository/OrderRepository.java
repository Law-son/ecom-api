package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Order;
import com.eyarko.ecom.entity.OrderItem;
import com.eyarko.ecom.entity.OrderStatus;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.entity.User;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for orders and their items.
 */
@Repository
public class OrderRepository {
    private static final String ORDER_SELECT =
        "SELECT order_id, user_id, order_date, status, total_amount FROM orders";

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final RowMapper<Order> orderRowMapper = (rs, rowNum) -> Order.builder()
        .id(rs.getLong("order_id"))
        .user(User.builder().id(rs.getLong("user_id")).build())
        .orderDate(toInstant(rs.getTimestamp("order_date")))
        .status(OrderStatus.valueOf(rs.getString("status")))
        .totalAmount(rs.getBigDecimal("total_amount"))
        .items(new ArrayList<>())
        .build();

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * Finds an order by id and loads its items.
     *
     * @param id order id
     * @return optional order
     */
    public Optional<Order> findById(Long id) {
        List<Order> orders = jdbcTemplate.query(
            ORDER_SELECT + " WHERE order_id = ?",
            orderRowMapper,
            id
        );
        if (orders.isEmpty()) {
            return Optional.empty();
        }
        attachItems(orders);
        return Optional.of(orders.get(0));
    }

    /**
     * Lists orders with pagination.
     *
     * @param pageable paging options
     * @return list of orders
     */
    public List<Order> findAll(Pageable pageable) {
        List<Order> orders = jdbcTemplate.query(
            ORDER_SELECT + " " + orderAndPage(pageable),
            orderRowMapper,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        attachItems(orders);
        return orders;
    }

    /**
     * Lists orders by user with pagination.
     *
     * @param userId user id
     * @param pageable paging options
     * @return list of orders
     */
    public List<Order> findByUserId(Long userId, Pageable pageable) {
        List<Order> orders = jdbcTemplate.query(
            ORDER_SELECT + " WHERE user_id = ? " + orderAndPage(pageable),
            orderRowMapper,
            userId,
            pageable.getPageSize(),
            pageable.getOffset()
        );
        attachItems(orders);
        return orders;
    }

    /**
     * Inserts or updates an order and persists items on insert.
     *
     * @param order order to save
     * @return saved order
     */
    public Order save(Order order) {
        if (order.getId() == null) {
            return insertOrder(order);
        }
        return updateOrder(order);
    }

    private void insertItems(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        String sql =
            "INSERT INTO order_items (order_id, product_id, quantity, unit_price, price_at_time) "
                + "VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(
            sql,
            order.getItems(),
            order.getItems().size(),
            (ps, item) -> {
                ps.setLong(1, order.getId());
                ps.setLong(2, item.getProduct().getId());
                ps.setInt(3, item.getQuantity());
                ps.setBigDecimal(4, item.getUnitPrice());
                ps.setBigDecimal(5, item.getPriceAtTime());
            }
        );
    }

    private void attachItems(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        Map<Long, Order> byId = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        for (Order order : orders) {
            byId.put(order.getId(), order);
            ids.add(order.getId());
            if (order.getItems() == null) {
                order.setItems(new ArrayList<>());
            } else {
                order.getItems().clear();
            }
        }
        String sql =
            "SELECT oi.order_item_id, oi.order_id, oi.product_id, oi.quantity, "
                + "oi.unit_price, oi.price_at_time, p.name AS product_name "
                + "FROM order_items oi JOIN products p ON p.product_id = oi.product_id "
                + "WHERE oi.order_id IN (:ids) ORDER BY oi.order_item_id ASC";
        namedJdbcTemplate.query(
            sql,
            new MapSqlParameterSource("ids", ids),
            (rs) -> {
                Long orderId = rs.getLong("order_id");
                Order order = byId.get(orderId);
                if (order == null) {
                    return;
                }
                OrderItem item = OrderItem.builder()
                    .id(rs.getLong("order_item_id"))
                    .product(Product.builder()
                        .id(rs.getLong("product_id"))
                        .name(rs.getString("product_name"))
                        .build())
                    .quantity(rs.getInt("quantity"))
                    .unitPrice(rs.getBigDecimal("unit_price"))
                    .priceAtTime(rs.getBigDecimal("price_at_time"))
                    .build();
                order.getItems().add(item);
            }
        );
    }

    private String orderAndPage(Pageable pageable) {
        String orderBy = "ORDER BY " + resolveSort(pageable.getSort());
        return orderBy + " LIMIT ? OFFSET ?";
    }

    private String resolveSort(Sort sort) {
        if (sort == null || sort.isEmpty()) {
            return "order_date DESC";
        }
        Sort.Order order = sort.iterator().next();
        String column = switch (order.getProperty()) {
            case "orderDate" -> "order_date";
            case "totalAmount" -> "total_amount";
            case "status" -> "status";
            default -> "order_date";
        };
        return column + " " + order.getDirection().name();
    }

    private static java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Timestamp toTimestamp(java.time.Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
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

    private static boolean isPostgres(java.sql.Connection connection) {
        try {
            String dbName = connection.getMetaData().getDatabaseProductName();
            return dbName != null && dbName.toLowerCase().contains("postgres");
        } catch (Exception ex) {
            return false;
        }
    }

    private Order insertOrder(Order order) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            boolean postgres = isPostgres(connection);
            String sql;
            if (order.getOrderDate() == null) {
                sql = postgres
                    ? "INSERT INTO orders (user_id, status, total_amount) "
                        + "VALUES (?, CAST(? AS order_status), ?)"
                    : "INSERT INTO orders (user_id, status, total_amount) VALUES (?, ?, ?)";
            } else {
                sql = postgres
                    ? "INSERT INTO orders (user_id, order_date, status, total_amount) "
                        + "VALUES (?, ?, CAST(? AS order_status), ?)"
                    : "INSERT INTO orders (user_id, order_date, status, total_amount) VALUES (?, ?, ?, ?)";
            }
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, order.getUser().getId());
            if (order.getOrderDate() == null) {
                ps.setString(2, (order.getStatus() == null ? OrderStatus.PENDING : order.getStatus()).name());
                ps.setBigDecimal(3, order.getTotalAmount());
            } else {
                ps.setTimestamp(2, toTimestamp(order.getOrderDate()));
                ps.setString(3, (order.getStatus() == null ? OrderStatus.PENDING : order.getStatus()).name());
                ps.setBigDecimal(4, order.getTotalAmount());
            }
            return ps;
        }, keyHolder);
        Long key = extractKey(keyHolder, "order_id");
        if (key == null) {
            return order;
        }
        order.setId(key);
        insertItems(order);
        return findById(order.getId()).orElse(order);
    }

    private Order updateOrder(Order order) {
        jdbcTemplate.update(connection -> {
            boolean postgres = isPostgres(connection);
            String sql = postgres
                ? "UPDATE orders SET user_id = ?, status = CAST(? AS order_status), total_amount = ? "
                    + "WHERE order_id = ?"
                : "UPDATE orders SET user_id = ?, status = ?, total_amount = ? WHERE order_id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, order.getUser().getId());
            ps.setString(2, order.getStatus().name());
            ps.setBigDecimal(3, order.getTotalAmount());
            ps.setLong(4, order.getId());
            return ps;
        });
        return findById(order.getId()).orElse(order);
    }
}


