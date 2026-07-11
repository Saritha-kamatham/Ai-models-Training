package com.fooddelivery.serviceimpl;

import com.fooddelivery.dto.OrderRequest;
import com.fooddelivery.dto.OrderResponse;
import com.fooddelivery.dto.OrderStatusUpdateRequest;
import com.fooddelivery.entity.*;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.*;
import com.fooddelivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public OrderResponse placeOrder(OrderRequest request, Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        List<CartItem> cartItems = cartItemRepository.findByUserId(customerId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty! Add items before checking out.");
        }

        // Validate that all food items belong to the same restaurant
        Restaurant restaurant = cartItems.get(0).getFood().getRestaurant();
        for (CartItem item : cartItems) {
            if (!item.getFood().getRestaurant().getId().equals(restaurant.getId())) {
                throw new BadRequestException("All items in the cart must belong to the same restaurant (" + restaurant.getName() + ")!");
            }
        }

        double totalAmount = cartItems.stream()
                .mapToDouble(CartItem::getTotalPrice)
                .sum();

        // Map Payment status based on method
        String paymentStatus = "PENDING";
        if (request.getPaymentMethod().equalsIgnoreCase("CARD") || request.getPaymentMethod().equalsIgnoreCase("UPI")) {
            paymentStatus = "COMPLETED"; // Simulated instant success
        }

        // Create Order
        Order order = Order.builder()
                .customer(customer)
                .restaurant(restaurant)
                .totalAmount(totalAmount)
                .deliveryAddress(request.getDeliveryAddress())
                .paymentStatus(paymentStatus)
                .deliveryStatus("PLACED")
                .orderedAt(LocalDateTime.now())
                .build();

        // Create OrderItems
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .food(cartItem.getFood())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .build();
            orderItems.add(orderItem);
        }
        order.setOrderItems(orderItems);

        // Save order (which cascades to OrderItems)
        Order savedOrder = orderRepository.save(order);

        // Create Payment record
        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod(request.getPaymentMethod().toUpperCase())
                .paymentStatus(paymentStatus)
                .transactionId(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .build();
        paymentRepository.save(payment);

        // Clear user's cart
        cartItemRepository.deleteByUserId(customerId);

        return mapToDto(savedOrder);
    }

    @Override
    public OrderResponse getOrderById(Long id, Long userId) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Security check
        boolean authorized = false;
        if (user.getRole().equals("ADMIN")) {
            authorized = true;
        } else if (user.getRole().equals("CUSTOMER") && order.getCustomer().getId().equals(userId)) {
            authorized = true;
        } else if (user.getRole().equals("OWNER") && order.getRestaurant().getOwner().getId().equals(userId)) {
            authorized = true;
        }

        if (!authorized) {
            throw new UnauthorizedException("You are not authorized to view this order!");
        }

        return mapToDto(order);
    }

    @Override
    public List<OrderResponse> getOrderHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Order> orders;
        if (user.getRole().equals("ADMIN")) {
            orders = orderRepository.findAllByOrderByOrderedAtDesc();
        } else if (user.getRole().equals("OWNER")) {
            // Find all restaurants owned by this owner
            List<Restaurant> restaurants = restaurantRepository.findByOwnerId(userId);
            orders = new ArrayList<>();
            for (Restaurant r : restaurants) {
                orders.addAll(orderRepository.findByRestaurantIdOrderByOrderedAtDesc(r.getId()));
            }
            // Sort merged lists
            orders.sort((o1, o2) -> o2.getOrderedAt().compareTo(o1.getOrderedAt()));
        } else {
            // CUSTOMER
            orders = orderRepository.findByCustomerIdOrderByOrderedAtDesc(userId);
        }

        return orders.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (request.getDeliveryStatus() != null && !request.getDeliveryStatus().isBlank()) {
            order.setDeliveryStatus(request.getDeliveryStatus().toUpperCase());
        }

        if (request.getPaymentStatus() != null && !request.getPaymentStatus().isBlank()) {
            String newPaymentStatus = request.getPaymentStatus().toUpperCase();
            order.setPaymentStatus(newPaymentStatus);
            
            // Sync with payment table
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (payment != null) {
                payment.setPaymentStatus(newPaymentStatus);
                paymentRepository.save(payment);
            }
        }

        Order updatedOrder = orderRepository.save(order);
        return mapToDto(updatedOrder);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if authorized to cancel
        if (user.getRole().equals("CUSTOMER")) {
            if (!order.getCustomer().getId().equals(userId)) {
                throw new UnauthorizedException("You are not authorized to cancel this order!");
            }
            if (!order.getDeliveryStatus().equals("PLACED")) {
                throw new BadRequestException("You can only cancel order before it starts preparation (current status: " + order.getDeliveryStatus() + ")!");
            }
        } else if (user.getRole().equals("OWNER")) {
            if (!order.getRestaurant().getOwner().getId().equals(userId)) {
                throw new UnauthorizedException("You are not authorized to cancel this order!");
            }
        }

        order.setDeliveryStatus("CANCELLED");
        order.setPaymentStatus("FAILED");
        
        // Sync with payment
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null) {
            payment.setPaymentStatus("FAILED");
            paymentRepository.save(payment);
        }

        orderRepository.save(order);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByOrderedAtDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private OrderResponse mapToDto(Order order) {
        List<OrderResponse.OrderItemDto> items = order.getOrderItems().stream()
                .map(item -> OrderResponse.OrderItemDto.builder()
                        .foodId(item.getFood().getId())
                        .foodName(item.getFood().getName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .imageUrl(item.getFood().getImageUrl())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getFullName())
                .restaurantId(order.getRestaurant().getId())
                .restaurantName(order.getRestaurant().getName())
                .totalAmount(order.getTotalAmount())
                .deliveryAddress(order.getDeliveryAddress())
                .paymentStatus(order.getPaymentStatus())
                .deliveryStatus(order.getDeliveryStatus())
                .orderedAt(order.getOrderedAt())
                .items(items)
                .build();
    }
}
