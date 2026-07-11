package com.fooddelivery.controller;

import com.fooddelivery.dto.OrderRequest;
import com.fooddelivery.dto.OrderResponse;
import com.fooddelivery.dto.OrderStatusUpdateRequest;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@CrossOrigin
public class OrderController {

    private final OrderService orderService;

    private User getLoggedInUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new UnauthorizedException("User not authenticated");
    }

    @PostMapping("/add")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        User user = getLoggedInUser();
        OrderResponse response = orderService.placeOrder(request, user.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders() {
        User user = getLoggedInUser();
        return ResponseEntity.ok(orderService.getOrderHistory(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        User user = getLoggedInUser();
        return ResponseEntity.ok(orderService.getOrderById(id, user.getId()));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id, @RequestBody OrderStatusUpdateRequest request) {
        User user = getLoggedInUser();
        
        // Ensure only Restaurant Owners or Admins update order status
        if (!user.getRole().equals("ADMIN") && !user.getRole().equals("OWNER")) {
            throw new UnauthorizedException("Only restaurant owners or admins can update order status!");
        }

        OrderResponse updated = orderService.updateOrderStatus(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> cancelOrder(@PathVariable Long id) {
        User user = getLoggedInUser();
        orderService.cancelOrder(id, user.getId());
        return ResponseEntity.ok("Order cancelled successfully.");
    }
}
