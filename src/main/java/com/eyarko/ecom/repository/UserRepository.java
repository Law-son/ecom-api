package com.eyarko.ecom.repository;

import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.entity.UserRole;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * JDBC repository for user persistence.
 */
@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> rowMapper = (rs, rowNum) -> User.builder()
        .id(rs.getLong("user_id"))
        .fullName(rs.getString("full_name"))
        .email(rs.getString("email"))
        .passwordHash(rs.getString("password_hash"))
        .role(UserRole.valueOf(rs.getString("role")))
        .createdAt(toInstant(rs.getTimestamp("created_at")))
        .lastLogin(toInstant(rs.getTimestamp("last_login")))
        .build();

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Finds a user by id.
     *
     * @param id user id
     * @return optional user
     */
    public Optional<User> findById(Long id) {
        return jdbcTemplate.query(
            "SELECT user_id, full_name, email, password_hash, role, created_at, last_login "
                + "FROM users WHERE user_id = ?",
            rowMapper,
            id
        ).stream().findFirst();
    }

    /**
     * Finds a user by email (case-insensitive).
     *
     * @param email email address
     * @return optional user
     */
    public Optional<User> findByEmail(String email) {
        return jdbcTemplate.query(
            "SELECT user_id, full_name, email, password_hash, role, created_at, last_login "
                + "FROM users WHERE LOWER(email) = LOWER(?)",
            rowMapper,
            email
        ).stream().findFirst();
    }

    /**
     * Lists all users ordered by creation date.
     *
     * @return list of users
     */
    public List<User> findAll() {
        return jdbcTemplate.query(
            "SELECT user_id, full_name, email, password_hash, role, created_at, last_login "
                + "FROM users ORDER BY created_at DESC",
            rowMapper
        );
    }

    /**
     * Inserts or updates a user.
     *
     * @param user user to save
     * @return saved user
     */
    public User save(User user) {
        UserRole role = user.getRole() == null ? UserRole.CUSTOMER : user.getRole();
        if (user.getId() == null) {
            return insertUser(user, role);
        }
        return updateUser(user, role);
    }

    /**
     * Checks if a user exists by id.
     *
     * @param id user id
     * @return true if present
     */
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE user_id = ?",
            Integer.class,
            id
        );
        return count != null && count > 0;
    }

    /**
     * Deletes a user by id.
     *
     * @param id user id
     */
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", id);
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

    private User insertUser(User user, UserRole role) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            boolean postgres = isPostgres(connection);
            String sql = postgres
                ? "INSERT INTO users (full_name, email, password_hash, role, last_login) "
                    + "VALUES (?, ?, ?, CAST(? AS user_role), ?)"
                : "INSERT INTO users (full_name, email, password_hash, role, last_login) "
                    + "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, role.name());
            ps.setTimestamp(5, toTimestamp(user.getLastLogin()));
            return ps;
        }, keyHolder);
        Long key = extractKey(keyHolder, "user_id");
        if (key == null) {
            return user;
        }
        return findById(key).orElse(user);
    }

    private User updateUser(User user, UserRole role) {
        jdbcTemplate.update(connection -> {
            boolean postgres = isPostgres(connection);
            String sql = postgres
                ? "UPDATE users SET full_name = ?, email = ?, password_hash = ?, role = CAST(? AS user_role), "
                    + "last_login = ? WHERE user_id = ?"
                : "UPDATE users SET full_name = ?, email = ?, password_hash = ?, role = ?, last_login = ? "
                    + "WHERE user_id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, role.name());
            ps.setTimestamp(5, toTimestamp(user.getLastLogin()));
            ps.setLong(6, user.getId());
            return ps;
        });
        return findById(user.getId()).orElse(user);
    }
}


