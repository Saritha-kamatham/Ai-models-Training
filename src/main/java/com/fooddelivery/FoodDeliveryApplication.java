package com.fooddelivery;

import com.fooddelivery.entity.*;
import com.fooddelivery.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
public class FoodDeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodDeliveryApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(
            UserRepository userRepository,
            RestaurantRepository restaurantRepository,
            FoodRepository foodRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            CartItemRepository cartItemRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Seed Admin user
            User admin = userRepository.findByEmail("admin@fooddelivery.com").orElse(null);
            if (admin == null) {
                admin = User.builder()
                        .email("admin@fooddelivery.com")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("App Administrator")
                        .phone("9999999999")
                        .address("Main Admin Hub")
                        .city("Tech City")
                        .role("ADMIN")
                        .build();
                admin = userRepository.save(admin);
                System.out.println("Seeded Default Admin: admin@fooddelivery.com / admin123");
            }

            // 2. Seed Restaurant Owner user
            User owner = userRepository.findByEmail("owner@fooddelivery.com").orElse(null);
            if (owner == null) {
                owner = User.builder()
                        .email("owner@fooddelivery.com")
                        .password(passwordEncoder.encode("owner123"))
                        .fullName("Chef Owner")
                        .phone("8888888888")
                        .address("Gourmet Street")
                        .city("Food Ville")
                        .role("OWNER")
                        .build();
                owner = userRepository.save(owner);
                System.out.println("Seeded Default Restaurant Owner: owner@fooddelivery.com / owner123");
            }

            // 3. Seed Customer user
            User customer = userRepository.findByEmail("customer@fooddelivery.com").orElse(null);
            if (customer == null) {
                customer = User.builder()
                        .email("customer@fooddelivery.com")
                        .password(passwordEncoder.encode("customer123"))
                        .fullName("John Doe")
                        .phone("7777777777")
                        .address("Apt 4B, Central Avenue")
                        .city("Bangalore")
                        .role("CUSTOMER")
                        .build();
                customer = userRepository.save(customer);
                System.out.println("Seeded Default Customer: customer@fooddelivery.com / customer123");
            }

            // Clean up old database records if coordinates/new cities are missing to force refresh
            boolean needsCoordinateReset = restaurantRepository.findAll().stream().anyMatch(r -> r.getLatitude() == null);
            if (restaurantRepository.count() <= 13 || needsCoordinateReset) {
                System.out.println("Cleaning up old database records to seed new cities (Bangalore, Mumbai, Madanapalle, Anantapur, Pune, Hyderabad)...");
                cartItemRepository.deleteAll();
                paymentRepository.deleteAll();
                orderRepository.deleteAll();
                foodRepository.deleteAll();
                restaurantRepository.deleteAll();
            }

            // 4. Seed Restaurants with Cities and Coordinates
            if (restaurantRepository.count() == 0) {
                // Bangalore Restaurants
                Restaurant r1 = Restaurant.builder()
                        .name("Pizza Palace")
                        .owner(owner)
                        .location("Koramangala, Bangalore")
                        .cuisine("Italian & Pizzas")
                        .rating(4.8)
                        .imageUrl("https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=500&q=80")
                        .latitude(12.9352)
                        .longitude(77.6245)
                        .build();

                Restaurant r2 = Restaurant.builder()
                        .name("Burger Point")
                        .owner(owner)
                        .location("Indiranagar, Bangalore")
                        .cuisine("Burgers & Fast Food")
                        .rating(4.5)
                        .imageUrl("https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=500&q=80")
                        .latitude(12.9719)
                        .longitude(77.6412)
                        .build();

                Restaurant r3 = Restaurant.builder()
                        .name("Biryani Zone")
                        .owner(owner)
                        .location("Jayanagar, Bangalore")
                        .cuisine("Biryani & North Indian")
                        .rating(4.7)
                        .imageUrl("https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?auto=format&fit=crop&w=500&q=80")
                        .latitude(12.9308)
                        .longitude(77.5838)
                        .build();

                // Mumbai Restaurants
                Restaurant r4 = Restaurant.builder()
                        .name("Tikka Town")
                        .owner(owner)
                        .location("Colaba, Mumbai")
                        .cuisine("Kebabs & Mughlai")
                        .rating(4.6)
                        .imageUrl("https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=500&q=80")
                        .latitude(18.9218)
                        .longitude(72.8347)
                        .build();

                Restaurant r5 = Restaurant.builder()
                        .name("Cafe Bombay")
                        .owner(owner)
                        .location("Bandra, Mumbai")
                        .cuisine("Continental & Desserts")
                        .rating(4.4)
                        .imageUrl("https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=500&q=80")
                        .latitude(19.0596)
                        .longitude(72.8295)
                        .build();

                Restaurant r6 = Restaurant.builder()
                        .name("Wok Express")
                        .owner(owner)
                        .location("Fort, Mumbai")
                        .cuisine("Chinese & Asian")
                        .rating(4.3)
                        .imageUrl("https://images.unsplash.com/photo-1525755662778-989d0524087e?auto=format&fit=crop&w=500&q=80")
                        .latitude(18.9340)
                        .longitude(72.8372)
                        .build();

                // Madanapalle Restaurants
                Restaurant r7 = Restaurant.builder()
                        .name("Sri Krishna Grand")
                        .owner(owner)
                        .location("Kadirappa Road, Madanapalle")
                        .cuisine("South Indian & Biryanis")
                        .rating(4.7)
                        .imageUrl("https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=500&q=80")
                        .latitude(13.5548)
                        .longitude(78.5015)
                        .build();

                Restaurant r8 = Restaurant.builder()
                        .name("Raju Gari Dhaba")
                        .owner(owner)
                        .location("Kadiri Road Bypass, Madanapalle")
                        .cuisine("Andhra Meals & Starters")
                        .rating(4.5)
                        .imageUrl("https://images.unsplash.com/photo-1626777552726-4a6b54c97e46?auto=format&fit=crop&w=500&q=80")
                        .latitude(13.5620)
                        .longitude(78.5045)
                        .build();

                Restaurant r9 = Restaurant.builder()
                        .name("Bake Zone Cafe")
                        .owner(owner)
                        .location("Nehru Street, Madanapalle")
                        .cuisine("Cakes, Burgers & Pizzas")
                        .rating(4.4)
                        .imageUrl("https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=500&q=80")
                        .latitude(13.5572)
                        .longitude(78.4985)
                        .build();

                // Anantapur Restaurants
                Restaurant r10 = Restaurant.builder()
                        .name("Ananta Biryani Durbar")
                        .owner(owner)
                        .location("Subash Road, Anantapur")
                        .cuisine("Biryani & Mandi")
                        .rating(4.6)
                        .imageUrl("https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?auto=format&fit=crop&w=500&q=80")
                        .latitude(14.6819)
                        .longitude(77.6006)
                        .build();

                Restaurant r11 = Restaurant.builder()
                        .name("Sri Srinivasa Grand")
                        .owner(owner)
                        .location("Clock Tower, Anantapur")
                        .cuisine("South Indian Veg Meals")
                        .rating(4.5)
                        .imageUrl("https://images.unsplash.com/photo-1610192244261-3f33de3f55e4?auto=format&fit=crop&w=500&q=80")
                        .latitude(14.6835)
                        .longitude(77.5985)
                        .build();

                // Pune Restaurants
                Restaurant r12 = Restaurant.builder()
                        .name("Pune Irani Cafe")
                        .owner(owner)
                        .location("Deccan Gymkhana, Pune")
                        .cuisine("Irani Chai, Bun Maska & Snacks")
                        .rating(4.8)
                        .imageUrl("https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=500&q=80")
                        .latitude(18.5178)
                        .longitude(73.8422)
                        .build();

                Restaurant r13 = Restaurant.builder()
                        .name("Sujata Mastani")
                        .owner(owner)
                        .location("Sadashiv Peth, Pune")
                        .cuisine("Ice Cream & Desserts")
                        .rating(4.7)
                        .imageUrl("https://images.unsplash.com/photo-1563729784474-d77dbb933a9e?auto=format&fit=crop&w=500&q=80")
                        .latitude(18.5085)
                        .longitude(73.8488)
                        .build();

                // Hyderabad Restaurants
                Restaurant r14 = Restaurant.builder()
                        .name("Bawarchi Restaurant")
                        .owner(owner)
                        .location("RTC X Roads, Hyderabad")
                        .cuisine("Hyderabadi Biryani & Mughlai")
                        .rating(4.8)
                        .imageUrl("https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?auto=format&fit=crop&w=500&q=80")
                        .latitude(17.4062)
                        .longitude(78.4891)
                        .build();

                Restaurant r15 = Restaurant.builder()
                        .name("Paradise Biryani")
                        .owner(owner)
                        .location("Secunderabad, Hyderabad")
                        .cuisine("Biryani & Kebabs")
                        .rating(4.6)
                        .imageUrl("https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=500&q=80")
                        .latitude(17.4436)
                        .longitude(78.4972)
                        .build();

                Restaurant r16 = Restaurant.builder()
                        .name("Subhan Bakery")
                        .owner(owner)
                        .location("Nampally, Hyderabad")
                        .cuisine("Bakery, Cookies & Snacks")
                        .rating(4.7)
                        .imageUrl("https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=500&q=80")
                        .latitude(17.3875)
                        .longitude(78.4682)
                        .build();

                r1 = restaurantRepository.save(r1);
                r2 = restaurantRepository.save(r2);
                r3 = restaurantRepository.save(r3);
                r4 = restaurantRepository.save(r4);
                r5 = restaurantRepository.save(r5);
                r6 = restaurantRepository.save(r6);
                r7 = restaurantRepository.save(r7);
                r8 = restaurantRepository.save(r8);
                r9 = restaurantRepository.save(r9);
                r10 = restaurantRepository.save(r10);
                r11 = restaurantRepository.save(r11);
                r12 = restaurantRepository.save(r12);
                r13 = restaurantRepository.save(r13);
                r14 = restaurantRepository.save(r14);
                r15 = restaurantRepository.save(r15);
                r16 = restaurantRepository.save(r16);
                System.out.println("Seeded Location Restaurants successfully!");

                // 5. Seed Foods
                Food f1 = Food.builder()
                        .name("Margherita Pizza")
                        .category("Pizzas")
                        .description("Classic pizza with fresh mozzarella, basil leaves, and tomato sauce")
                        .price(299.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1604068549290-dea0e4a305ca?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r1)
                        .build();

                Food f2 = Food.builder()
                        .name("Double Pepperoni Pizza")
                        .category("Pizzas")
                        .description("Loaded with spicy pepperoni slices and extra mozzarella cheese")
                        .price(399.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1628840042765-356cda07504e?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r1)
                        .build();

                Food f3 = Food.builder()
                        .name("Classic Cheese Burger")
                        .category("Burgers")
                        .description("Flame-grilled beef patty, cheddar cheese, lettuce, and pickles")
                        .price(199.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r2)
                        .build();

                Food f4 = Food.builder()
                        .name("Truffle French Fries")
                        .category("Sides")
                        .description("Crispy fries tossed with white truffle oil, sea salt, and parmesan")
                        .price(149.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1576107232684-1279f390859f?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r2)
                        .build();

                Food f5 = Food.builder()
                        .name("Hyderabadi Chicken Biryani")
                        .category("Biryani")
                        .description("Basmati rice cooked with chicken, aromatic spices, and saffron")
                        .price(320.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r3)
                        .build();

                Food f6 = Food.builder()
                        .name("Spicy Chicken Tikka")
                        .category("Kebabs")
                        .description("Boneless chicken chunks marinated in yogurt and spices, grilled in tandoor")
                        .price(260.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r4)
                        .build();

                Food f7 = Food.builder()
                        .name("Chocolate Fudge Waffle")
                        .category("Desserts")
                        .description("Warm waffle topped with dark chocolate fudge sauce and vanilla ice cream")
                        .price(180.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1563729784474-d77dbb933a9e?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r5)
                        .build();

                Food f8 = Food.builder()
                        .name("Veg Hakka Noodles")
                        .category("Noodles")
                        .description("Stir-fried noodles with crisp vegetables and savory soy garlic sauce")
                        .price(220.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1525755662778-989d0524087e?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r6)
                        .build();

                // Madanapalle Food Items
                Food f9 = Food.builder()
                        .name("Ghee Butter Masala Dosa")
                        .category("South Indian")
                        .description("Crispy dosa roasted in pure ghee, stuffed with potato masala, served with chutneys")
                        .price(80.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r7)
                        .build();

                Food f10 = Food.builder()
                        .name("Madanapalle Chicken Biryani")
                        .category("Biryani")
                        .description("Fragrant basmati rice layered with spiced native chicken masala cooked in local style")
                        .price(250.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1633945274405-b6c8069047b0?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r7)
                        .build();

                Food f11 = Food.builder()
                        .name("Andhra Pappu Meals")
                        .category("Meals")
                        .description("Authentic Andhra lunch platter with hot rice, ghee, tomato pappu, charu, and fry")
                        .price(150.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1610192244261-3f33de3f55e4?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r8)
                        .build();

                Food f12 = Food.builder()
                        .name("Spicy Guntur Chicken Fry")
                        .category("Sides")
                        .description("Tender chicken pieces fried with spicy Guntur red chillies and curry leaves")
                        .price(220.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1598515214211-89d3e73ae83b?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r8)
                        .build();

                Food f13 = Food.builder()
                        .name("Crispy Veg Burger")
                        .category("Burgers")
                        .description("Crispy vegetable patty, local mayo, lettuce, and onions inside warm sesame bun")
                        .price(110.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1586190848861-99aa4a171e90?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r9)
                        .build();

                // Anantapur Food Items
                Food f14 = Food.builder()
                        .name("Ananta Mandi Biryani")
                        .category("Biryani")
                        .description("Flavourful Arabian Mandi rice served with slow-cooked spicy tandoori chicken")
                        .price(290.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r10)
                        .build();

                Food f15 = Food.builder()
                        .name("Sri Srinivasa South Indian Thali")
                        .category("Meals")
                        .description("Traditional unlimited South Indian meals with rice, sambar, rasam, curd, and sweets")
                        .price(160.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1610192244261-3f33de3f55e4?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r11)
                        .build();

                // Pune Food Items
                Food f16 = Food.builder()
                        .name("Special Bun Maska & Irani Chai")
                        .category("Sides")
                        .description("Freshly baked soft bun stuffed with butter, served alongside authentic strong Irani tea")
                        .price(90.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r12)
                        .build();

                Food f17 = Food.builder()
                        .name("Mango Mastani Shake")
                        .category("Desserts")
                        .description("Thick mango milkshake topped with a giant scoop of vanilla ice cream, dry fruits, and cherry")
                        .price(140.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1563729784474-d77dbb933a9e?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r13)
                        .build();

                // Hyderabad Food Items
                Food f18 = Food.builder()
                        .name("Bawarchi Special Mutton Biryani")
                        .category("Biryani")
                        .description("World-famous Hyderabadi mutton biryani layered with marinated meat, basmati rice, and ghee")
                        .price(380.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r14)
                        .build();

                Food f19 = Food.builder()
                        .name("Paradise Double Masala Chicken Biryani")
                        .category("Biryani")
                        .description("Richly spiced and heavily flavored chicken biryani prepared in the signature Paradise style")
                        .price(340.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1633945274405-b6c8069047b0?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r15)
                        .build();

                Food f20 = Food.builder()
                        .name("Subhan Bakery Osmania Biscuits")
                        .category("Cookies")
                        .description("Box of legendary, melt-in-the-mouth sweet and salty Osmania biscuits baked fresh daily")
                        .price(120.0)
                        .available(true)
                        .imageUrl("https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=300&q=80")
                        .restaurant(r16)
                        .build();

                f1 = foodRepository.save(f1);
                f2 = foodRepository.save(f2);
                f3 = foodRepository.save(f3);
                f4 = foodRepository.save(f4);
                f5 = foodRepository.save(f5);
                f6 = foodRepository.save(f6);
                f7 = foodRepository.save(f7);
                f8 = foodRepository.save(f8);
                foodRepository.save(f9);
                foodRepository.save(f10);
                foodRepository.save(f11);
                foodRepository.save(f12);
                foodRepository.save(f13);
                foodRepository.save(f14);
                foodRepository.save(f15);
                foodRepository.save(f16);
                foodRepository.save(f17);
                foodRepository.save(f18);
                foodRepository.save(f19);
                foodRepository.save(f20);
                System.out.println("Seeded all menu items successfully!");

                // 6. Seed active sample order
                Order order = Order.builder()
                        .customer(customer)
                        .restaurant(r1)
                        .totalAmount(698.0)
                        .deliveryAddress("Apt 4B, Central Avenue, Metropolis")
                        .paymentStatus("COMPLETED")
                        .deliveryStatus("PREPARING")
                        .orderedAt(LocalDateTime.now().minusMinutes(30))
                        .build();

                List<OrderItem> items = new ArrayList<>();
                items.add(OrderItem.builder().order(order).food(f1).quantity(1).price(f1.getPrice()).build());
                items.add(OrderItem.builder().order(order).food(f2).quantity(1).price(f2.getPrice()).build());
                order.setOrderItems(items);

                Order savedOrder = orderRepository.save(order);

                Payment payment = Payment.builder()
                        .order(savedOrder)
                        .paymentMethod("CARD")
                        .paymentStatus("COMPLETED")
                        .transactionId(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                        .build();
                paymentRepository.save(payment);
                System.out.println("Seeded sample order!");
            }
        };
    }
}
