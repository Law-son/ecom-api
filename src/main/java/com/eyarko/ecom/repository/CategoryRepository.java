package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.Category;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for category persistence.
 */
@Repository
public class CategoryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Category> rowMapper = (rs, rowNum) -> Category.builder()
        .id(rs.getLong("category_id"))
        .name(rs.getString("category_name"))
        .createdAt(toInstant(rs.getTimestamp("created_at")))
        .build();

    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds a category by id.
     *
     * @param id category id
     * @return optional category
     */
    public Optional<Category> findById(Long id) {
        return jdbcTemplate.query(
            "SELECT category_id, category_name, created_at FROM categories WHERE category_id = ?",
            rowMapper,
            id
        ).stream().findFirst();
    }

    /**
     * Lists all categories ordered by name.
     *
     * @return list of categories
     */
    public List<Category> findAll() {
        return jdbcTemplate.query(
            "SELECT category_id, category_name, created_at FROM categories ORDER BY category_name ASC",
            rowMapper
        );
    }

    /**
     * Inserts or updates a category.
     *
     * @param category category to save
     * @return saved category
     */
    public Category save(Category category) {
        if (category.getId() == null) {
            return insertCategory(category);
        }
        return updateCategory(category);
    }

    /**
     * Checks if a category exists by id.
     *
     * @param id category id
     * @return true if present
     */
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM categories WHERE category_id = ?",
            Integer.class,
            id
        );
        return count != null && count > 0;
    }

    /**
     * Deletes a category by id.
     *
     * @param id category id
     */
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM categories WHERE category_id = ?", id);
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

    private Category insertCategory(Category category) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO categories (category_name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, category.getName());
            return ps;
        }, keyHolder);
        Long key = extractKey(keyHolder, "category_id");
        if (key == null) {
            return category;
        }
        return findById(key).orElse(category);
    }

    private Category updateCategory(Category category) {
        jdbcTemplate.update(
            "UPDATE categories SET category_name = ? WHERE category_id = ?",
            category.getName(),
            category.getId()
        );
        return findById(category.getId()).orElse(category);
    }
}


