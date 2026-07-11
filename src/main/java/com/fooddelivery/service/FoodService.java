package com.fooddelivery.service;

import com.fooddelivery.dto.FoodDto;
import com.fooddelivery.entity.Food;

import java.util.List;

public interface FoodService {
    FoodDto saveFood(FoodDto foodDto);
    List<FoodDto> getFoods();
    FoodDto getFoodById(Long id);
    FoodDto updateFood(Long id, FoodDto foodDto);
    void deleteFood(Long id);
    List<FoodDto> getFoodsByRestaurant(Long restaurantId);
    List<FoodDto> searchFood(String keyword);
    
    // Internal helper
    Food findFoodById(Long id);
}
