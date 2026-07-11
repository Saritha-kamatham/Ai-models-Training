package com.fooddelivery.serviceimpl;

import com.fooddelivery.dto.AddToCartRequest;
import com.fooddelivery.dto.CartItemDto;
import com.fooddelivery.dto.CartResponse;
import com.fooddelivery.entity.CartItem;
import com.fooddelivery.entity.Food;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.repository.CartItemRepository;
import com.fooddelivery.repository.FoodRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final FoodRepository foodRepository;

    @Override
    @Transactional
    public CartResponse addToCart(AddToCartRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Food food = foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + request.getFoodId()));

        if (!food.getAvailable()) {
            throw new BadRequestException("Food item is currently not available!");
        }

        // Check if item is already in user's cart
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndFoodId(userId, request.getFoodId());
        
        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(cartItem);
        } else {
            CartItem cartItem = CartItem.builder()
                    .user(user)
                    .food(food)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(cartItem);
        }

        return getCart(userId);
    }

    @Override
    public CartResponse getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        
        List<CartItemDto> itemDtos = items.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        Double grandTotal = items.stream()
                .mapToDouble(CartItem::getTotalPrice)
                .sum();

        return CartResponse.builder()
                .items(itemDtos)
                .grandTotal(grandTotal)
                .build();
    }

    @Override
    @Transactional
    public CartResponse updateCart(Long cartItemId, Integer quantity, Long userId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (!cartItem.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this cart item!");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
        } else {
            cartItem.setQuantity(quantity);
            cartItemRepository.save(cartItem);
        }

        return getCart(userId);
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(Long cartItemId, Long userId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (!cartItem.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to remove this cart item!");
        }

        cartItemRepository.delete(cartItem);
        return getCart(userId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    @Override
    public Double calculateTotal(Long userId) {
        return cartItemRepository.findByUserId(userId).stream()
                .mapToDouble(CartItem::getTotalPrice)
                .sum();
    }

    private CartItemDto mapToDto(CartItem cartItem) {
        return CartItemDto.builder()
                .id(cartItem.getId())
                .foodId(cartItem.getFood().getId())
                .foodName(cartItem.getFood().getName())
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())
                .totalPrice(cartItem.getTotalPrice())
                .imageUrl(cartItem.getFood().getImageUrl())
                .build();
    }
}
