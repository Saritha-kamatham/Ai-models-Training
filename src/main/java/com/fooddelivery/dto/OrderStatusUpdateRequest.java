package com.fooddelivery.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdateRequest {
    private String deliveryStatus;
    private String paymentStatus;
}
