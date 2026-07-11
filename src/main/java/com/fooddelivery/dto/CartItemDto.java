package com.fooddelivery.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDto {
    private Long id;
    private Long foodId;
    private String foodName;
    private Double price;
    private Integer quantity;
    private Double totalPrice;
    private String imageUrl;
}
