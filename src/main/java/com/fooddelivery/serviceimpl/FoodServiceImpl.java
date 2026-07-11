package com.fooddelivery.serviceimpl;

import com.fooddelivery.dto.FoodDto;
import com.fooddelivery.entity.Food;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.FoodRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {

    private final FoodRepository foodRepository;
    private final RestaurantRepository restaurantRepository;

    @Override
    public FoodDto saveFood(FoodDto foodDto) {
        Restaurant restaurant = restaurantRepository.findById(foodDto.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + foodDto.getRestaurantId()));

        Food food = Food.builder()
                .name(foodDto.getName())
                .category(foodDto.getCategory())
                .description(foodDto.getDescription())
                .price(foodDto.getPrice())
                .available(foodDto.getAvailable() != null ? foodDto.getAvailable() : true)
                .imageUrl(foodDto.getImageUrl())
                .restaurant(restaurant)
                .build();

        Food savedFood = foodRepository.save(food);
        return mapToDto(savedFood);
    }

    @Override
    public List<FoodDto> getFoods() {
        return foodRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public FoodDto getFoodById(Long id) {
        Food food = findFoodById(id);
        return mapToDto(food);
    }

    @Override
    public FoodDto updateFood(Long id, FoodDto foodDto) {
        Food food = findFoodById(id);

        food.setName(foodDto.getName());
        food.setCategory(foodDto.getCategory());
        food.setDescription(foodDto.getDescription());
        food.setPrice(foodDto.getPrice());
        if (foodDto.getAvailable() != null) {
            food.setAvailable(foodDto.getAvailable());
        }
        if (foodDto.getImageUrl() != null) {
            food.setImageUrl(foodDto.getImageUrl());
        }

        Food updatedFood = foodRepository.save(food);
        return mapToDto(updatedFood);
    }

    @Override
    public void deleteFood(Long id) {
        Food food = findFoodById(id);
        foodRepository.delete(food);
    }

    @Override
    public List<FoodDto> getFoodsByRestaurant(Long restaurantId) {
        return foodRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FoodDto> searchFood(String keyword) {
        return foodRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(keyword, keyword).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public Food findFoodById(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food not found with id: " + id));
    }

    private FoodDto mapToDto(Food food) {
        return FoodDto.builder()
                .id(food.getId())
                .name(food.getName())
                .category(food.getCategory())
                .description(food.getDescription())
                .price(food.getPrice())
                .available(food.getAvailable())
                .imageUrl(food.getImageUrl())
                .restaurantId(food.getRestaurant().getId())
                .restaurantName(food.getRestaurant().getName())
                .build();
    }
}
