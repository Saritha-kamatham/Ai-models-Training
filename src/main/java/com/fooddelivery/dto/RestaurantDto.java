package com.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantDto {
    private Long id;

    @NotBlank(message = "Restaurant name is required")
    private String name;

    private Long ownerId;
    private String ownerName;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Cuisine type is required")
    private String cuisine;

    private Double rating;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
}
