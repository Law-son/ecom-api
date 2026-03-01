-- EXPLAIN ANALYZE templates for index validation.
-- Replace placeholders (e.g., :categoryId) with real values.

-- Optional: create function-based indexes for case-insensitive search (PostgreSQL).
-- CREATE INDEX IF NOT EXISTS idx_products_name_lower ON products (LOWER(name));
-- CREATE INDEX IF NOT EXISTS idx_categories_name_lower ON categories (LOWER(category_name));

-- Products by category with sorting (idx_products_category_id, idx_products_avg_rating)
EXPLAIN ANALYZE
SELECT p.product_id, p.name, p.price, p.avg_rating
FROM products p
WHERE p.category_id = :categoryId
ORDER BY p.avg_rating DESC
LIMIT 20 OFFSET 0;

-- Products by name search (idx_products_name_lower)
-- Example replacement: :searchTerm -> phone
EXPLAIN ANALYZE
SELECT p.product_id, p.name, p.price
FROM products p
WHERE LOWER(p.name) LIKE CONCAT('%', LOWER(:searchTerm), '%')
ORDER BY p.name ASC
LIMIT 20 OFFSET 0;

-- Categories by name (idx_categories_name_lower)
-- Example replacement: :categoryTerm -> electronics
EXPLAIN ANALYZE
SELECT c.category_id, c.category_name
FROM categories c
WHERE LOWER(c.category_name) LIKE CONCAT('%', LOWER(:categoryTerm), '%')
ORDER BY c.category_name ASC;

-- Orders by user (idx_orders_user_id)
EXPLAIN ANALYZE
SELECT o.order_id, o.order_date, o.total_amount, o.status
FROM orders o
WHERE o.user_id = :userId
ORDER BY o.order_date DESC
LIMIT 20 OFFSET 0;

