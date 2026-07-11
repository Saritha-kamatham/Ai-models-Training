package com.fooddelivery.service;

import com.fooddelivery.dto.RestaurantDto;
import com.fooddelivery.entity.Restaurant;

import java.util.List;

public interface RestaurantService {
    RestaurantDto saveRestaurant(RestaurantDto restaurantDto, Long ownerId);
    List<RestaurantDto> getRestaurants();
    RestaurantDto getRestaurantById(Long id);
    RestaurantDto updateRestaurant(Long id, RestaurantDto restaurantDto);
    void deleteRestaurant(Long id);
    List<RestaurantDto> searchRestaurant(String keyword);
    List<RestaurantDto> getRestaurantsByOwner(Long ownerId);
    
    // Internal helper
    Restaurant findRestaurantById(Long id);
}
