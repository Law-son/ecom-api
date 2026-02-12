-- Smart Ecommerce System Database Schema
-- Updated: 2026-01-17

-- =========================
-- DROP TABLES (Dependency Order)
-- =========================

DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS inventory;

-- Drop dependent tables FIRST
DROP TABLE IF EXISTS reviews;

DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

-- =========================
-- DROP ENUM TYPES
-- =========================

DROP TYPE IF EXISTS order_status;
DROP TYPE IF EXISTS user_role;

-- =========================
-- CREATE ENUMS
-- =========================

CREATE TYPE user_role AS ENUM ('CUSTOMER', 'ADMIN');
CREATE TYPE order_status AS ENUM (
    'PENDING',
    'RECEIVED',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
);

-- =========================
-- USERS TABLE
-- =========================

CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- =========================
-- CATEGORIES TABLE
-- =========================

CREATE TABLE categories (
    category_id BIGSERIAL PRIMARY KEY,
    category_name VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- PRODUCTS TABLE
-- =========================

CREATE TABLE products (
    product_id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(category_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    image_url TEXT,
    avg_rating DECIMAL(3, 2) DEFAULT 0,
    review_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- INVENTORY TABLE
-- =========================

CREATE TABLE inventory (
    inventory_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT UNIQUE NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    inventory_status VARCHAR(50) NOT NULL DEFAULT 'Out of stock',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- =========================
-- CART TABLES
-- =========================

CREATE TABLE carts (
    cart_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cart_items (
    cart_item_id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES carts(cart_id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (cart_id, product_id)
);

-- =========================
-- ORDERS TABLE
-- =========================

CREATE TABLE orders (
    order_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status order_status NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(12, 2) NOT NULL CHECK (total_amount >= 0),
    version BIGINT NOT NULL DEFAULT 0
);

-- =========================
-- ORDER ITEMS TABLE
-- =========================

CREATE TABLE order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL CHECK (unit_price >= 0),
    price_at_time DECIMAL(10, 2) NOT NULL CHECK (price_at_time >= 0)
);

-- =========================
-- INDEXES
-- =========================

CREATE INDEX idx_products_name_lower ON products (LOWER(name));
CREATE INDEX idx_categories_name_lower ON categories (LOWER(category_name));
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_avg_rating ON products (avg_rating DESC);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_carts_user_id ON carts (user_id);
CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items (product_id);
CREATE INDEX idx_orders_user_id ON orders (user_id);

-- =========================
-- SAMPLE DATA
-- =========================

INSERT INTO categories (category_name) VALUES
('Electronics'),
('Clothing'),
('Home'),
('Books'),
('Devices');

INSERT INTO users (full_name, email, password_hash, role) VALUES
('Admin User', 'admin@example.com', 'qwerty@12345', 'ADMIN'),
('John Doe', 'john@example.com', 'qwerty@12345', 'CUSTOMER');
