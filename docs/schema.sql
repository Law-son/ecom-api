-- Smart Ecommerce System Database Schema
-- Updated: 2026-01-17

-- Drop tables if they exist (in reverse order of dependencies)
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

-- Drop enums if they exist
DROP TYPE IF EXISTS order_status;
DROP TYPE IF EXISTS user_role;

-- Create Enums
CREATE TYPE user_role AS ENUM ('CUSTOMER', 'ADMIN');
CREATE TYPE order_status AS ENUM ('PENDING', 'SHIPPED', 'DELIVERED', 'CANCELLED');

-- Create Tables

-- 1. Users Table
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Optional audit metadata
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;

-- 2. Categories Table
CREATE TABLE categories (
    category_id SERIAL PRIMARY KEY,
    category_name VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Products Table
CREATE TABLE products (
    product_id SERIAL PRIMARY KEY,
    category_id INT NOT NULL REFERENCES categories(category_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Rating summary derived from MongoDB reviews
ALTER TABLE products ADD COLUMN IF NOT EXISTS avg_rating DECIMAL(3, 2) DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS review_count INT DEFAULT 0;

-- 4. Inventory Table
CREATE TABLE inventory (
    inventory_id SERIAL PRIMARY KEY,
    product_id INT UNIQUE NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Orders Table
CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status order_status NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(12, 2) NOT NULL CHECK (total_amount >= 0)
);

-- 6. Order Items Table
CREATE TABLE order_items (
    order_item_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id INT NOT NULL REFERENCES products(product_id),
    quantity INT NOT NULL CHECK (quantity > 0),
    price_at_time DECIMAL(10, 2) NOT NULL CHECK (price_at_time >= 0)
);

-- Create Indices for Performance

-- For faster search by product name (case-insensitive)
CREATE INDEX idx_products_name_lower ON products (LOWER(name));

-- For faster search by category name (case-insensitive)
CREATE INDEX idx_categories_name_lower ON categories (LOWER(category_name));

-- For faster filtering by category
CREATE INDEX idx_products_category_id ON products (category_id);

-- For faster rating-based sorting
CREATE INDEX IF NOT EXISTS idx_products_avg_rating ON products (avg_rating DESC);

-- For faster user lookups by email
CREATE INDEX idx_users_email ON users (email);

-- For faster order lookups by user
CREATE INDEX idx_orders_user_id ON orders (user_id);

-- Insert Sample Data

-- Sample Categories
INSERT INTO categories (category_name) VALUES 
('Electronics'), 
('Clothing'), 
('Home & Garden'), 
('Books'), 
('Toys');

-- Sample Admin User (password is 'admin123' hashed - placeholder)
INSERT INTO users (full_name, email, password_hash, role) VALUES 
('Admin User', 'admin@example.com', 'hashed_password_here', 'ADMIN');

-- Sample Customer User (password is 'customer123' hashed - placeholder)
INSERT INTO users (full_name, email, password_hash, role) VALUES 
('John Doe', 'john@example.com', 'hashed_password_here', 'CUSTOMER');


