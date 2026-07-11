package com.fooddelivery.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long orderId;
    private Long customerId;
    private String customerName;
    private Long restaurantId;
    private String restaurantName;
    private Double totalAmount;
    private String deliveryAddress;
    private String paymentStatus;
    private String deliveryStatus;
    private LocalDateTime orderedAt;
    private List<OrderItemDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemDto {
        private Long foodId;
        private String foodName;
        private Integer quantity;
        private Double price;
        private String imageUrl;
    }
}
