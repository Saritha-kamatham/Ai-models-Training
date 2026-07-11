package com.fooddelivery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodDto {
    private Long id;

    @NotNull(message = "Restaurant ID is required")
    private Long restaurantId;
    
    private String restaurantName;

    @NotBlank(message = "Food name is required")
    private String name;

    @NotBlank(message = "Category is required")
    private String category;

    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be positive")
    private Double price;

    @NotNull(message = "Availability status is required")
    private Boolean available;

    private String imageUrl;
}
