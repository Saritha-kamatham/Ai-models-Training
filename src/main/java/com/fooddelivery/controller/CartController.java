package com.fooddelivery.controller;

import com.fooddelivery.dto.AddToCartRequest;
import com.fooddelivery.dto.CartItemDto;
import com.fooddelivery.dto.CartResponse;
import com.fooddelivery.entity.CartItem;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.CartItemRepository;
import com.fooddelivery.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@CrossOrigin
public class CartController {

    private final CartService cartService;
    private final CartItemRepository cartItemRepository;

    private User getLoggedInUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new UnauthorizedException("User not authenticated");
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        User user = getLoggedInUser();
        CartResponse response = cartService.addToCart(request, user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        User user = getLoggedInUser();
        return ResponseEntity.ok(cartService.getCart(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartItemDto> getCartItem(@PathVariable Long id) {
        User user = getLoggedInUser();
        CartItem cartItem = cartItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + id));

        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to view this cart item!");
        }

        CartItemDto dto = CartItemDto.builder()
                .id(cartItem.getId())
                .foodId(cartItem.getFood().getId())
                .foodName(cartItem.getFood().getName())
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())
                .totalPrice(cartItem.getTotalPrice())
                .imageUrl(cartItem.getFood().getImageUrl())
                .build();

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<CartResponse> updateCartItem(@PathVariable Long id, @RequestParam Integer quantity) {
        User user = getLoggedInUser();
        CartResponse response = cartService.updateCart(id, quantity, user.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<CartResponse> deleteCartItem(@PathVariable Long id) {
        User user = getLoggedInUser();
        CartResponse response = cartService.removeFromCart(id, user.getId());
        return ResponseEntity.ok(response);
    }
}
