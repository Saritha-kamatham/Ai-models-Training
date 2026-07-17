package com.fooddelivery.service;

import com.fooddelivery.dto.ChefRecommendationDto;
import com.fooddelivery.dto.DemandForecastDto;
import com.fooddelivery.dto.TopDishDto;

import java.util.List;
import java.util.Map;

public interface AIService {
    List<TopDishDto> getTopDishes();
    List<DemandForecastDto> getDemandForecast();
    List<ChefRecommendationDto> getChefRecommendations();
    Map<String, String> getIngredientEstimation();
}
