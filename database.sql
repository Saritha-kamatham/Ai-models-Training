-- ====================================================================
-- FOOD DELIVERY APPLICATION DATABASE SCHEMA & SEED DATA
-- ====================================================================

CREATE DATABASE IF NOT EXISTS food_delivery_db;
USE food_delivery_db;

-- 1. Create Users Table (Customers, Owners, Admins)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address TEXT,
    city VARCHAR(100),
    role VARCHAR(50) NOT NULL
);

-- 2. Create Restaurants Table
CREATE TABLE IF NOT EXISTS restaurants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_id BIGINT NOT NULL,
    location VARCHAR(255) NOT NULL,
    cuisine VARCHAR(255) NOT NULL,
    rating DOUBLE DEFAULT 0.0,
    image_url VARCHAR(255),
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Create Foods Table
CREATE TABLE IF NOT EXISTS foods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    description TEXT,
    price DOUBLE NOT NULL,
    available BOOLEAN DEFAULT TRUE,
    image_url VARCHAR(255),
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
);

-- 4. Create Cart Items Table
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (food_id) REFERENCES foods(id) ON DELETE CASCADE
);

-- 5. Create Orders Table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    total_amount DOUBLE NOT NULL,
    delivery_address TEXT NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    delivery_status VARCHAR(50) NOT NULL,
    ordered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE
);

-- 6. Create Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price DOUBLE NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (food_id) REFERENCES foods(id) ON DELETE CASCADE
);

-- 7. Create Payments Table
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    payment_method VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- 8. Create Meal Plans Table
CREATE TABLE IF NOT EXISTS meal_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    price DOUBLE NOT NULL,
    description TEXT
);

-- 9. Create Subscriptions Table
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    meal_plan_id BIGINT NOT NULL,
    duration_days INT NOT NULL,
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (meal_plan_id) REFERENCES meal_plans(id) ON DELETE CASCADE
);

-- ====================================================================
-- SAMPLE INSERT STATEMENTS (PASSWORD IS BCRYPT FOR 'password123')
-- ====================================================================

-- Seeding Users
-- Admin (admin@fooddelivery.com / admin123) -> encoded via application startup
-- Restaurant Owner (owner@fooddelivery.com / owner123) -> encoded via application startup
-- Customer (customer@fooddelivery.com / customer123) -> encoded via application startup

-- Note: Below inserts assume User IDs 1 (Admin), 2 (Owner), and 3 (Customer) are created by application CommandLineRunner.
-- If running manually, we seed some standard users:
-- BCrypt encoded values for 'password123': '$2a$10$nCo.G7v281n7G9j9PzHl5exsAeg7K5VdOqC.yv5eO3sMbeXlK/eS6'
INSERT INTO users (id, email, password, full_name, phone, address, city, role) 
VALUES 
(4, 'pizza_palace_owner@fooddelivery.com', '$2a$10$nCo.G7v281n7G9j9PzHl5exsAeg7K5VdOqC.yv5eO3sMbeXlK/eS6', 'Mario Rossi', '9876543210', '12 Via Roma', 'Metropolis', 'OWNER'),
(5, 'alice@fooddelivery.com', '$2a$10$nCo.G7v281n7G9j9PzHl5exsAeg7K5VdOqC.yv5eO3sMbeXlK/eS6', 'Alice Green', '9081726354', '456 Oak Avenue', 'Tech City', 'CUSTOMER');

-- Seeding Restaurants
INSERT INTO restaurants (id, name, owner_id, location, cuisine, rating, image_url)
VALUES 
(1, 'Pizza Palace', 4, 'Downtown Core', 'Italian & Pizzas', 4.7, 'https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=500&q=80'),
(2, 'Burger Point', 2, 'West Side', 'Burgers & Fast Food', 4.5, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=500&q=80'),
(3, 'Sushi Zen', 2, 'East Plaza', 'Japanese Sushi', 4.8, 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?auto=format&fit=crop&w=500&q=80'),
(4, 'Taco Loco', 4, 'South Crossing', 'Mexican Tacos', 4.2, 'https://images.unsplash.com/photo-1565299585323-38d6b0865b47?auto=format&fit=crop&w=500&q=80');

-- Seeding Foods
INSERT INTO foods (id, restaurant_id, name, category, description, price, available, image_url)
VALUES 
-- Pizza Palace Items
(1, 1, 'Margherita Pizza', 'Pizzas', 'Classic pizza with fresh mozzarella, basil leaves, and tomato sauce', 12.99, TRUE, 'https://images.unsplash.com/photo-1604068549290-dea0e4a305ca?auto=format&fit=crop&w=300&q=80'),
(2, 1, 'Double Pepperoni Pizza', 'Pizzas', 'Loaded with spicy pepperoni slices and extra mozzarella cheese', 15.99, TRUE, 'https://images.unsplash.com/photo-1628840042765-356cda07504e?auto=format&fit=crop&w=300&q=80'),
(3, 1, 'Garlic Bread Sticks', 'Sides', 'Warm baked dough strips brushed with garlic butter and parsley', 5.49, TRUE, 'https://images.unsplash.com/photo-1544982503-9f984c14501a?auto=format&fit=crop&w=300&q=80'),

-- Burger Point Items
(4, 2, 'Classic Cheese Burger', 'Burgers', 'Flame-grilled beef patty, cheddar cheese, lettuce, pickles, and house sauce', 8.99, TRUE, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=300&q=80'),
(5, 2, 'Spicy Crispy Chicken Burger', 'Burgers', 'Deep fried breaded chicken breast topped with hot pepper sauce and jalapeños', 9.99, TRUE, 'https://images.unsplash.com/photo-1625813506062-0aeb1d7a094b?auto=format&fit=crop&w=300&q=80'),
(6, 2, 'Basket of Truffle Fries', 'Sides', 'Golden fries tossed with white truffle oil, sea salt, and parmesan cheese', 6.49, TRUE, 'https://images.unsplash.com/photo-1576107232684-1279f390859f?auto=format&fit=crop&w=300&q=80'),

-- Sushi Zen Items
(7, 3, 'Premium Dragon Roll', 'Sushi', 'Eel and cucumber inside, sliced avocado and unagi sauce on top', 18.99, TRUE, 'https://images.unsplash.com/photo-1611143669185-af224c5e3252?auto=format&fit=crop&w=300&q=80'),
(8, 3, 'Salmon Nigiri (2 Pcs)', 'Sushi', 'Fresh slices of raw Atlantic salmon over seasoned vinegar rice', 7.99, TRUE, 'https://images.unsplash.com/photo-1583623025817-d180a2221d0a?auto=format&fit=crop&w=300&q=80'),

-- Taco Loco Items
(9, 4, 'Baja Fish Tacos (3 Pcs)', 'Tacos', 'Crispy beer-battered fish fillets in corn tortillas with chipotle slaw', 11.49, TRUE, 'https://images.unsplash.com/photo-1565299585323-38d6b0865b47?auto=format&fit=crop&w=300&q=80'),
(10, 4, 'Loaded Nachos Grande', 'Appetizers', 'Tortilla chips covered in warm queso, black beans, pico, and sour cream', 10.99, FALSE, 'https://images.unsplash.com/photo-1513456852971-30c0b8199d4d?auto=format&fit=crop&w=300&q=80');

-- Seeding Meal Plans
INSERT INTO meal_plans (id, name, type, price, description)
VALUES
(1, 'Veg Meal Plan', 'Veg Meal', 2999.00, 'Healthy vegetarian meals for daily nutrition'),
(2, 'Protein Meal Plan', 'Protein Meal', 3999.00, 'High protein meals optimized for fitness enthusiasts'),
(3, 'Family Feast Plan', 'Family Meal', 6999.00, 'Substantial meal packages suitable for families');

-- Seeding Subscriptions
INSERT INTO subscriptions (id, customer_id, meal_plan_id, duration_days, status)
VALUES
(1, 5, 1, 30, 'ACTIVE'),
(2, 5, 2, 15, 'ACTIVE');
