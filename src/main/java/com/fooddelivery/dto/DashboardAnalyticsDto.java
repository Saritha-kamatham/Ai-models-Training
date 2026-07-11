package com.fooddelivery.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardAnalyticsDto {
    private Long totalCustomers;
    private Long totalRestaurants;
    private Long totalFoods;
    private Long totalOrders;
    private Double totalRevenue;
}
