package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.UpdateRestaurantRequest;
import com.restaurantmanager.entity.FoodType;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.exception.BadRequestException;
import com.restaurantmanager.exception.ResourceNotFoundException;
import com.restaurantmanager.repository.MenuItemRepository;
import com.restaurantmanager.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    @Transactional(readOnly = true)
    public Restaurant getById(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));
    }

    @Transactional(readOnly = true)
    public Restaurant getBySlug(String slug) {
        return restaurantRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + slug));
    }

    @Transactional
    public Restaurant update(UUID restaurantId, UpdateRestaurantRequest request) {
        Restaurant restaurant = getById(restaurantId);
        // Turning on veg-only is only allowed once every existing item is vegetarian.
        if (request.vegOnly() && !restaurant.isVegOnly()
                && menuItemRepository.existsByRestaurantIdAndFoodTypeNot(restaurantId, FoodType.VEG)) {
            throw new BadRequestException(
                    "Remove or convert all non-vegetarian and egg items before switching to a vegetarian-only restaurant.");
        }
        restaurant.setName(request.name());
        restaurant.setAddress(request.address());
        restaurant.setPhone(request.phone());
        restaurant.setVegOnly(request.vegOnly());
        restaurant.setLogoUrl(request.logoUrl());
        restaurant.setThemeColor(request.themeColor());
        restaurant.setTagline(request.tagline());
        return restaurant;
    }
}
