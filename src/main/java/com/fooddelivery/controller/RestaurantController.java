package com.fooddelivery.controller;

import com.fooddelivery.dto.RestaurantDto;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
@CrossOrigin
public class RestaurantController {

    private final RestaurantService restaurantService;

    private User getLoggedInUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new UnauthorizedException("User not authenticated");
    }

    @PostMapping("/add")
    public ResponseEntity<RestaurantDto> addRestaurant(@Valid @RequestBody RestaurantDto restaurantDto) {
        User user = getLoggedInUser();
        Long ownerId = user.getId();
        
        // Admin can specify a different owner if needed, otherwise uses their own
        if (user.getRole().equals("ADMIN") && restaurantDto.getOwnerId() != null) {
            ownerId = restaurantDto.getOwnerId();
        } else if (!user.getRole().equals("OWNER") && !user.getRole().equals("ADMIN")) {
            throw new UnauthorizedException("Only restaurant owners or admins can add restaurants!");
        }

        RestaurantDto createdRestaurant = restaurantService.saveRestaurant(restaurantDto, ownerId);
        return new ResponseEntity<>(createdRestaurant, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RestaurantDto>> getAllRestaurants() {
        return ResponseEntity.ok(restaurantService.getRestaurants());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantDto> getRestaurantById(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getRestaurantById(id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<RestaurantDto> updateRestaurant(@PathVariable Long id, @Valid @RequestBody RestaurantDto restaurantDto) {
        User user = getLoggedInUser();
        RestaurantDto existing = restaurantService.getRestaurantById(id);

        // Allow only the restaurant owner or Admin to update details
        if (!user.getRole().equals("ADMIN") && !existing.getOwnerId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to update this restaurant's details!");
        }

        return ResponseEntity.ok(restaurantService.updateRestaurant(id, restaurantDto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteRestaurant(@PathVariable Long id) {
        User user = getLoggedInUser();
        if (!user.getRole().equals("ADMIN")) {
            throw new UnauthorizedException("Only admins can delete restaurants!");
        }
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.ok("Restaurant deleted successfully.");
    }

    @GetMapping("/search")
    public ResponseEntity<List<RestaurantDto>> searchRestaurants(@RequestParam String keyword) {
        return ResponseEntity.ok(restaurantService.searchRestaurant(keyword));
    }

    @GetMapping("/owner")
    public ResponseEntity<List<RestaurantDto>> getMyRestaurants() {
        User user = getLoggedInUser();
        if (!user.getRole().equals("OWNER") && !user.getRole().equals("ADMIN")) {
            throw new UnauthorizedException("Only owners or admins can view registered owner restaurants.");
        }
        return ResponseEntity.ok(restaurantService.getRestaurantsByOwner(user.getId()));
    }
}
