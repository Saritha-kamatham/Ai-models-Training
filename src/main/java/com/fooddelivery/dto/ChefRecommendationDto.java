package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefRecommendationDto {
    private String dish;
    private String priority; // HIGH, MEDIUM, LOW
    private String action; // e.g. "Prepare more Margherita Pizza", "Maintain normal stock"
}
