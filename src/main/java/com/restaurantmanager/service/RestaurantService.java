package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.UpdateRestaurantRequest;
import com.restaurantmanager.entity.FoodType;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.exception.BadRequestException;
import com.restaurantmanager.exception.ResourceNotFoundException;
import com.restaurantmanager.repository.MenuItemRepository;
import com.restaurantmanager.repository.RestaurantRepository;
import com.restaurantmanager.util.ImageEnhancer;
import com.restaurantmanager.util.ImageSignature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private static final long MAX_LOGO_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_LOGO_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

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

    @Transactional
    public Restaurant updateLogo(UUID restaurantId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new BadRequestException("Logo image must be 2MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_LOGO_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Logo must be a PNG, JPEG, WEBP, or GIF image");
        }
        Restaurant restaurant = getById(restaurantId);
        byte[] originalBytes;
        try {
            originalBytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }
        if (!ImageSignature.matches(originalBytes, contentType)) {
            throw new BadRequestException("That file doesn't look like a valid image");
        }
        try {
            // Small/blurry uploads are common for logos — normalize to a standard resolution and
            // sharpen so they render crisply everywhere, rather than storing the raw upload as-is.
            restaurant.setLogoImage(ImageEnhancer.enhance(originalBytes));
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("That file doesn't look like a valid image");
        }
        restaurant.setLogoImageContentType("image/png");
        return restaurant;
    }

    @Transactional
    public Restaurant clearLogo(UUID restaurantId) {
        Restaurant restaurant = getById(restaurantId);
        restaurant.setLogoImage(null);
        restaurant.setLogoImageContentType(null);
        return restaurant;
    }
}
