package com.fooddelivery.controller;

import com.fooddelivery.dto.FoodDto;
import com.fooddelivery.dto.RestaurantDto;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.service.FoodService;
import com.fooddelivery.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/foods")
@RequiredArgsConstructor
@CrossOrigin
public class FoodController {

    private final FoodService foodService;
    private final RestaurantService restaurantService;

    private User getLoggedInUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new UnauthorizedException("User not authenticated");
    }

    @PostMapping("/add")
    public ResponseEntity<FoodDto> addFood(@Valid @RequestBody FoodDto foodDto) {
        User user = getLoggedInUser();
        RestaurantDto restaurant = restaurantService.getRestaurantById(foodDto.getRestaurantId());

        // Validate that owner owns this restaurant, or is Admin
        if (!user.getRole().equals("ADMIN") && !restaurant.getOwnerId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to add food items to this restaurant!");
        }

        FoodDto createdFood = foodService.saveFood(foodDto);
        return new ResponseEntity<>(createdFood, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<FoodDto>> getAllFoods() {
        return ResponseEntity.ok(foodService.getFoods());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodDto> getFoodById(@PathVariable Long id) {
        return ResponseEntity.ok(foodService.getFoodById(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<FoodDto> updateFood(@PathVariable Long id, @Valid @RequestBody FoodDto foodDto) {
        User user = getLoggedInUser();
        FoodDto existingFood = foodService.getFoodById(id);
        RestaurantDto restaurant = restaurantService.getRestaurantById(existingFood.getRestaurantId());

        if (!user.getRole().equals("ADMIN") && !restaurant.getOwnerId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to update this restaurant's menu items!");
        }

        return ResponseEntity.ok(foodService.updateFood(id, foodDto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFood(@PathVariable Long id) {
        User user = getLoggedInUser();
        FoodDto existingFood = foodService.getFoodById(id);
        RestaurantDto restaurant = restaurantService.getRestaurantById(existingFood.getRestaurantId());

        if (!user.getRole().equals("ADMIN") && !restaurant.getOwnerId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this restaurant's menu items!");
        }

        foodService.deleteFood(id);
        return ResponseEntity.ok("Food item deleted successfully.");
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<FoodDto>> getFoodsByRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(foodService.getFoodsByRestaurant(restaurantId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FoodDto>> searchFoods(@RequestParam String keyword) {
        return ResponseEntity.ok(foodService.searchFood(keyword));
    }
}
