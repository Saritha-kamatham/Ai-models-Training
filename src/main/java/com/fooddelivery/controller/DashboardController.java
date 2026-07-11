package com.fooddelivery.controller;

import com.fooddelivery.dto.DashboardAnalyticsDto;
import com.fooddelivery.entity.Food;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@CrossOrigin
public class DashboardController {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final FoodRepository foodRepository;
    private final OrderRepository orderRepository;

    private User getLoggedInUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new UnauthorizedException("User not authenticated");
    }

    @GetMapping("/analytics")
    public ResponseEntity<DashboardAnalyticsDto> getAnalytics() {
        User user = getLoggedInUser();

        if (user.getRole().equals("ADMIN")) {
            long totalCustomers = userRepository.findByRole("CUSTOMER").size();
            long totalRestaurants = restaurantRepository.count();
            long totalFoods = foodRepository.count();
            long totalOrders = orderRepository.count();

            double totalRevenue = orderRepository.findAll().stream()
                    .filter(order -> order.getPaymentStatus().equalsIgnoreCase("COMPLETED"))
                    .mapToDouble(Order::getTotalAmount)
                    .sum();

            DashboardAnalyticsDto analytics = DashboardAnalyticsDto.builder()
                    .totalCustomers(totalCustomers)
                    .totalRestaurants(totalRestaurants)
                    .totalFoods(totalFoods)
                    .totalOrders(totalOrders)
                    .totalRevenue(totalRevenue)
                    .build();

            return ResponseEntity.ok(analytics);
            
        } else if (user.getRole().equals("OWNER")) {
            List<Restaurant> myRestaurants = restaurantRepository.findByOwnerId(user.getId());
            long totalRestaurants = myRestaurants.size();

            long totalFoods = 0;
            long totalOrders = 0;
            double totalRevenue = 0.0;
            Set<Long> uniqueCustomerIds = new HashSet<>();

            for (Restaurant restaurant : myRestaurants) {
                // Count foods
                List<Food> foods = foodRepository.findByRestaurantId(restaurant.getId());
                totalFoods += foods.size();

                // Get orders
                List<Order> orders = orderRepository.findByRestaurantIdOrderByOrderedAtDesc(restaurant.getId());
                totalOrders += orders.size();

                for (Order order : orders) {
                    uniqueCustomerIds.add(order.getCustomer().getId());
                    if (order.getPaymentStatus().equalsIgnoreCase("COMPLETED")) {
                        totalRevenue += order.getTotalAmount();
                    }
                }
            }

            DashboardAnalyticsDto analytics = DashboardAnalyticsDto.builder()
                    .totalCustomers((long) uniqueCustomerIds.size())
                    .totalRestaurants(totalRestaurants)
                    .totalFoods(totalFoods)
                    .totalOrders(totalOrders)
                    .totalRevenue(totalRevenue)
                    .build();

            return ResponseEntity.ok(analytics);
        }

        throw new UnauthorizedException("Only owners and admins can view dashboard analytics!");
    }
}
