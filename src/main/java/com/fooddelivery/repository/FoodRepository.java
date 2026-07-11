package com.fooddelivery.repository;

import com.fooddelivery.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByRestaurantId(Long restaurantId);
    List<Food> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(String name, String category);
}
