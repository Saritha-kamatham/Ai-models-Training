package com.fooddelivery.service;

import com.fooddelivery.dto.OrderRequest;
import com.fooddelivery.dto.OrderResponse;
import com.fooddelivery.dto.OrderStatusUpdateRequest;

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(OrderRequest request, Long customerId);
    OrderResponse getOrderById(Long id, Long userId);
    List<OrderResponse> getOrderHistory(Long userId);
    OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request);
    void cancelOrder(Long orderId, Long userId);
    List<OrderResponse> getAllOrders();
}
