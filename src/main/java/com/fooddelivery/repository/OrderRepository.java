package com.fooddelivery.repository;

import com.fooddelivery.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerIdOrderByOrderedAtDesc(Long customerId);
    List<Order> findByRestaurantIdOrderByOrderedAtDesc(Long restaurantId);
    List<Order> findAllByOrderByOrderedAtDesc();
}
