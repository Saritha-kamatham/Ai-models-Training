package com.fooddelivery.serviceimpl;

import com.fooddelivery.dto.RestaurantDto;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    @Override
    public RestaurantDto saveRestaurant(RestaurantDto restaurantDto, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner user not found with id: " + ownerId));

        if (!owner.getRole().equals("OWNER") && !owner.getRole().equals("ADMIN")) {
            throw new BadRequestException("Only users with role OWNER or ADMIN can register a restaurant!");
        }

        Restaurant restaurant = Restaurant.builder()
                .name(restaurantDto.getName())
                .location(restaurantDto.getLocation())
                .cuisine(restaurantDto.getCuisine())
                .rating(restaurantDto.getRating() != null ? restaurantDto.getRating() : 0.0)
                .imageUrl(restaurantDto.getImageUrl())
                .latitude(restaurantDto.getLatitude())
                .longitude(restaurantDto.getLongitude())
                .owner(owner)
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        return mapToDto(savedRestaurant);
    }

    @Override
    public List<RestaurantDto> getRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public RestaurantDto getRestaurantById(Long id) {
        Restaurant restaurant = findRestaurantById(id);
        return mapToDto(restaurant);
    }

    @Override
    public RestaurantDto updateRestaurant(Long id, RestaurantDto restaurantDto) {
        Restaurant restaurant = findRestaurantById(id);

        restaurant.setName(restaurantDto.getName());
        restaurant.setLocation(restaurantDto.getLocation());
        restaurant.setCuisine(restaurantDto.getCuisine());
        if (restaurantDto.getRating() != null) {
            restaurant.setRating(restaurantDto.getRating());
        }
        if (restaurantDto.getImageUrl() != null) {
            restaurant.setImageUrl(restaurantDto.getImageUrl());
        }
        if (restaurantDto.getLatitude() != null) {
            restaurant.setLatitude(restaurantDto.getLatitude());
        }
        if (restaurantDto.getLongitude() != null) {
            restaurant.setLongitude(restaurantDto.getLongitude());
        }

        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);
        return mapToDto(updatedRestaurant);
    }

    @Override
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = findRestaurantById(id);
        restaurantRepository.delete(restaurant);
    }

    @Override
    public List<RestaurantDto> searchRestaurant(String keyword) {
        return restaurantRepository.findByNameContainingIgnoreCaseOrCuisineContainingIgnoreCaseOrLocationContainingIgnoreCase(
                keyword, keyword, keyword).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RestaurantDto> getRestaurantsByOwner(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public Restaurant findRestaurantById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
    }

    private RestaurantDto mapToDto(Restaurant restaurant) {
        return RestaurantDto.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .location(restaurant.getLocation())
                .cuisine(restaurant.getCuisine())
                .rating(restaurant.getRating())
                .imageUrl(restaurant.getImageUrl())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .ownerId(restaurant.getOwner().getId())
                .ownerName(restaurant.getOwner().getFullName())
                .build();
    }
}
