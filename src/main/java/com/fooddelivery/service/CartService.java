package com.fooddelivery.service;

import com.fooddelivery.dto.AddToCartRequest;
import com.fooddelivery.dto.CartResponse;

public interface CartService {
    CartResponse addToCart(AddToCartRequest request, Long userId);
    CartResponse getCart(Long userId);
    CartResponse updateCart(Long cartItemId, Integer quantity, Long userId);
    CartResponse removeFromCart(Long cartItemId, Long userId);
    void clearCart(Long userId);
    Double calculateTotal(Long userId);
}
