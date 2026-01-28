package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Category;
import com.eyarko.ecom.entity.Product;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for product persistence.
 */
@Repository
public class ProductRepository {
    private static final String BASE_SELECT =
        "SELECT p.product_id, p.category_id, c.category_name, p.name, p.description, p.price, p.image_url, "
            + "p.avg_rating, p.review_count, p.created_at "
            + "FROM products p JOIN categories c ON c.category_id = p.category_id";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Product> rowMapper = (rs, rowNum) -> Product.builder()
        .id(rs.getLong("product_id"))
        .category(Category.builder()
            .id(rs.getLong("category_id"))
            .name(rs.getString("category_name"))
            .build())
        .name(rs.getString("name"))
        .description(rs.getString("description"))
        .price(rs.getBigDecimal("price"))
        .imageUrl(rs.getString("image_url"))
        .avgRating(rs.getBigDecimal("avg_rating"))
        .reviewCount((Integer) rs.getObject("review_count"))
        .createdAt(toInstant(rs.getTimestamp("created_at")))
        .build();

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds a product by id with category info.
     *
     * @param id product id
     * @return optional product
     */
    public Optional<Product> findById(Long id) {
        return jdbcTemplate.query(
            BASE_SELECT + " WHERE p.product_id = ?",
            rowMapper,
            id
        ).stream().findFirst();
    }

    /**
     * Lists all products ordered by name.
     *
     * @return list of products
     */
    public List<Product> findAll() {
        return jdbcTemplate.query(
            BASE_SELECT + " ORDER BY p.name ASC",
            rowMapper
        );
    }

    /**
     * Lists products with pagination.
     *
     * @param pageable paging options
     * @return list of products
     */
    public List<Product> findAll(Pageable pageable) {
        return jdbcTemplate.query(
            BASE_SELECT + " " + orderAndPage(pageable),
            rowMapper,
            pageable.getPageSize(),
            pageable.getOffset()
        );
    }

    /**
     * Lists products filtered by category.
     *
     * @param categoryId category id
     * @param pageable paging options
     * @return list of products
     */
    public List<Product> findByCategoryId(Long categoryId, Pageable pageable) {
        return jdbcTemplate.query(
            BASE_SELECT + " WHERE p.category_id = ? " + orderAndPage(pageable),
            rowMapper,
            categoryId,
            pageable.getPageSize(),
            pageable.getOffset()
        );
    }

    /**
     * Lists products filtered by name.
     *
     * @param name name filter
     * @param pageable paging options
     * @return list of products
     */
    public List<Product> findByNameContainingIgnoreCase(String name, Pageable pageable) {
        String like = "%" + name.toLowerCase() + "%";
        return jdbcTemplate.query(
            BASE_SELECT + " WHERE LOWER(p.name) LIKE ? " + orderAndPage(pageable),
            rowMapper,
            like,
            pageable.getPageSize(),
            pageable.getOffset()
        );
    }

    /**
     * Lists products filtered by name or category name.
     *
     * @param name name filter
     * @param categoryName category name filter
     * @param pageable paging options
     * @return list of products
     */
    public List<Product> findByNameContainingIgnoreCaseOrCategory_NameContainingIgnoreCase(
        String name,
        String categoryName,
        Pageable pageable
    ) {
        String like = "%" + name.toLowerCase() + "%";
        String categoryLike = "%" + categoryName.toLowerCase() + "%";
        return jdbcTemplate.query(
            BASE_SELECT + " WHERE LOWER(p.name) LIKE ? OR LOWER(c.category_name) LIKE ? " + orderAndPage(pageable),
            rowMapper,
            like,
            categoryLike,
            pageable.getPageSize(),
            pageable.getOffset()
        );
    }

    /**
     * Lists products filtered by category and name.
     *
     * @param categoryId category id
     * @param name name filter
     * @param pageable paging options
     * @return list of products
     */
    public List<Product> findByCategoryIdAndNameContainingIgnoreCase(
        Long categoryId,
        String name,
        Pageable pageable
    ) {
        String like = "%" + name.toLowerCase() + "%";
        return jdbcTemplate.query(
            BASE_SELECT + " WHERE p.category_id = ? AND LOWER(p.name) LIKE ? " + orderAndPage(pageable),
            rowMapper,
            categoryId,
            like,
            pageable.getPageSize(),
            pageable.getOffset()
        );
    }

    /**
     * Inserts or updates a product.
     *
     * @param product product to save
     * @return saved product
     */
    public Product save(Product product) {
        if (product.getId() == null) {
            return insertProduct(product);
        }
        return updateProduct(product);
    }

    /**
     * Checks if a product exists by id.
     *
     * @param id product id
     * @return true if present
     */
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM products WHERE product_id = ?",
            Integer.class,
            id
        );
        return count != null && count > 0;
    }

    /**
     * Deletes a product by id.
     *
     * @param id product id
     */
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM products WHERE product_id = ?", id);
    }

    private String orderAndPage(Pageable pageable) {
        String orderBy = "ORDER BY " + resolveSort(pageable.getSort());
        return orderBy + " LIMIT ? OFFSET ?";
    }

    private String resolveSort(Sort sort) {
        if (sort == null || sort.isEmpty()) {
            return "p.name ASC";
        }
        Sort.Order order = sort.iterator().next();
        String column = switch (order.getProperty()) {
            case "price" -> "p.price";
            case "avgRating" -> "p.avg_rating";
            case "createdAt" -> "p.created_at";
            case "name" -> "p.name";
            default -> "p.name";
        };
        return column + " " + order.getDirection().name();
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

    private Product insertProduct(Product product) {
        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        BigDecimal avgRating = product.getAvgRating() == null ? BigDecimal.ZERO : product.getAvgRating();
        Integer reviewCount = product.getReviewCount() == null ? 0 : product.getReviewCount();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO products (category_id, name, description, price, image_url, avg_rating, review_count) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, categoryId);
            ps.setString(2, product.getName());
            ps.setString(3, product.getDescription());
            ps.setBigDecimal(4, product.getPrice());
            ps.setString(5, product.getImageUrl());
            ps.setBigDecimal(6, avgRating);
            ps.setInt(7, reviewCount);
            return ps;
        }, keyHolder);
        Long key = extractKey(keyHolder, "product_id");
        if (key == null) {
            return product;
        }
        return findById(key).orElse(product);
    }

    private Product updateProduct(Product product) {
        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        BigDecimal avgRating = product.getAvgRating() == null ? BigDecimal.ZERO : product.getAvgRating();
        Integer reviewCount = product.getReviewCount() == null ? 0 : product.getReviewCount();
        jdbcTemplate.update(
            "UPDATE products SET category_id = ?, name = ?, description = ?, price = ?, image_url = ?, "
                + "avg_rating = ?, review_count = ? WHERE product_id = ?",
            categoryId,
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getImageUrl(),
            avgRating,
            reviewCount,
            product.getId()
        );
        return findById(product.getId()).orElse(product);
    }
}


