package com.fooddelivery.controller;

import com.fooddelivery.dto.ChefRecommendationDto;
import com.fooddelivery.dto.DemandForecastDto;
import com.fooddelivery.dto.TopDishDto;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin
public class AIController {

    private final AIService aiService;

    private User getLoggedInUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new UnauthorizedException("User not authenticated");
    }

    private void verifyChefOrAdmin() {
        User user = getLoggedInUser();
        if (!user.getRole().equals("ADMIN") && !user.getRole().equals("OWNER")) {
            throw new UnauthorizedException("Access Denied! Administrators and Restaurant Owners only.");
        }
    }

    @GetMapping("/top-dishes")
    public ResponseEntity<List<TopDishDto>> getTopDishes() {
        verifyChefOrAdmin();
        return ResponseEntity.ok(aiService.getTopDishes());
    }

    @GetMapping("/demand-forecast")
    public ResponseEntity<List<DemandForecastDto>> getDemandForecast() {
        verifyChefOrAdmin();
        return ResponseEntity.ok(aiService.getDemandForecast());
    }

    @GetMapping("/chef-recommendations")
    public ResponseEntity<List<ChefRecommendationDto>> getChefRecommendations() {
        verifyChefOrAdmin();
        return ResponseEntity.ok(aiService.getChefRecommendations());
    }

    @GetMapping("/ingredient-estimation")
    public ResponseEntity<Map<String, String>> getIngredientEstimation() {
        verifyChefOrAdmin();
        return ResponseEntity.ok(aiService.getIngredientEstimation());
    }
}
