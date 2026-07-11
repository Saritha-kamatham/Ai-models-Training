# Food Delivery Application

A complete, production-grade, enterprise-ready Full Stack Food Delivery Web Application (similar to Swiggy/Zomato) built with **Java 21, Spring Boot 3.x, Hibernate, Spring Security, JWT, MySQL 8.x**, and a **Modern responsive vanilla HTML5/CSS3/ES6 JavaScript frontend** featuring glassmorphism elements, CSS variables, and dynamic micro-animations.

---

## Technical Stack

- **Backend**: Java 21, Spring Boot 3.x, Spring Web, Spring Data JPA, Hibernate, Spring Security + JJWT (JSON Web Token), Lombok, Jakarta Validation.
- **Frontend**: HTML5, CSS3 (CSS Variables, Grid, Flexbox, Keyframes), JavaScript (ES6+, Fetch API, Async/Await).
- **Database**: MySQL 8.x.
- **API Testing**: Postman Collection (included in root).
- **Build Tool**: Maven 3.9+.

---

## Folder Package Structure

```
food-delivery-app/
├── pom.xml                               # Maven project configuration
├── database.sql                          # Database creation & insert seeds
├── food-delivery-app.postman_collection  # Postman collection for API testing
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── fooddelivery/
│   │   │           ├── FoodDeliveryApplication.java
│   │   │           ├── config/           # Security, JWT, MVC mappings
│   │   │           ├── controller/       # REST controllers
│   │   │           ├── entity/           # Hibernate JPA models
│   │   │           ├── repository/       # Data Access Layer
│   │   │           ├── service/          # Service interfaces
│   │   │           ├── serviceimpl/      # Service business implementations
│   │   │           ├── dto/              # Request / Response structures
│   │   │           ├── exception/        # Exception handlers
│   │   │           └── utils/            # JWT Token utility classes
│   │   └── resources/
│   │       ├── application.properties    # Server & Data source properties
│   │       └── static/                   # Static Frontend web assets
│   │           ├── index.html            # Home landing
│   │           ├── login.html            # User login
│   │           ├── register.html         # User sign up
│   │           ├── restaurants.html      # Outlets listing
│   │           ├── restaurant-details.html # Dishes menu
│   │           ├── menu.html             # Explore dishes search
│   │           ├── cart.html             # Shopping cart
│   │           ├── checkout.html         # Billing address & checkout
│   │           ├── orders.html           # Active orders vertical timeline
│   │           ├── dashboard.html        # Merchant & Admin analytics panel
│   │           ├── 404.html              # Custom page missing
│   │           ├── css/                  # Styling files (glassmorphism)
│   │           └── js/                   # Page-specific frontend controllers
```

---

## Database Configuration & Setup

1. Make sure your local **MySQL Server** is running on port `3306`.
2. Open your MySQL client (e.g., MySQL Workbench, Command Line) and run the scripts in [database.sql](file:///C:/Users/Prasanth7799/.gemini/antigravity/scratch/food-delivery-app/database.sql).
   This script creates the database `food_delivery_db` and inserts sample restaurants, food menus, and users.
3. Database credentials configuration is defined in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=root
   ```
   *Modify these lines if your local MySQL instance has a different password.*

---

## Execution Instructions

1. Navigate to the project root directory:
   ```bash
   cd C:/Users/Prasanth7799/.gemini/antigravity/scratch/food-delivery-app
   ```
2. Build and package the application with Maven:
   ```bash
   mvn clean package
   ```
3. Run the Spring Boot application jar:
   ```bash
   mvn spring-boot:run
   ```
4. Access the frontend application directly in your web browser:
   **[http://localhost:8080](http://localhost:8080)**

---

## Default Seeded Accounts Credentials

When the application runs, a database CommandLineRunner auto-seeds these default users:

| Role | Email | Password |
|------|-------|----------|
| **Administrator** | `admin@fooddelivery.com` | `admin123` |
| **Restaurant Owner** | `owner@fooddelivery.com` | `owner123` |
| **Customer** | `customer@fooddelivery.com` | `customer123` |

---

## REST API Endpoints Map

### Auth APIs
- `POST /auth/register` (Public) - Register customer or owner.
- `POST /auth/login` (Public) - Authenticate credentials, returns JWT token.

### Customer Profiles (Secure)
- `POST /customers/add` (Admin) - Manual creation of profiles.
- `GET /customers` (Admin) - List all customers.
- `GET /customers/{id}` (Customer / Admin) - Fetch profile.
- `PUT /customers/update/{id}` (Customer / Admin) - Update address, phone, etc.
- `DELETE /customers/delete/{id}` (Admin) - Delete profile.

### Restaurants
- `GET /restaurants` (Public) - Browse outlets.
- `GET /restaurants/{id}` (Public) - Fetch restaurant details.
- `POST /restaurants/add` (Owner / Admin) - Register a new outlet.
- `PUT /restaurants/update/{id}` (Owner / Admin) - Update restaurant fields.
- `DELETE /restaurants/delete/{id}` (Admin) - Delete outlet.
- `GET /restaurants/search?keyword=query` (Public) - Keyword search.

### Food Menu
- `GET /foods` (Public) - List all dishes.
- `GET /foods/{id}` (Public) - View dish details.
- `POST /foods/add` (Owner / Admin) - Add food item to restaurant menu.
- `PUT /foods/update/{id}` (Owner / Admin) - Update price/availability.
- `DELETE /foods/delete/{id}` (Owner / Admin) - Remove food item from menu.
- `GET /foods/restaurant/{restaurantId}` (Public) - Get restaurant's menu list.
- `GET /foods/search?keyword=query` (Public) - Search foods.

### Shopping Cart (Secure Customer Only)
- `POST /cart/add` - Add item to cart.
- `GET /cart` - Fetch active cart items and totals.
- `PUT /cart/update/{id}?quantity=q` - Update quantity.
- `DELETE /cart/delete/{id}` - Remove item from cart.

### Orders & Tracking (Secure)
- `POST /orders/add` (Customer) - Checkout cart and place order.
- `GET /orders` (Role-based) - Lists history.
- `GET /orders/{id}` - Get order status details and items.
- `PUT /orders/update/{id}` (Owner / Admin) - Update delivery/payment status.
- `DELETE /orders/delete/{id}` (Customer / Owner) - Cancel order.

### Dashboard Analytics (Secure Owner / Admin)
- `GET /dashboard/analytics` - Return metrics counters.
