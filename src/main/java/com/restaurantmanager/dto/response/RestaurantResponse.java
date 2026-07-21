package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.entity.ThemeColor;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

public record RestaurantResponse(
        UUID id,
        String name,
        String slug,
        String address,
        String phone,
        boolean active,
        boolean vegOnly,
        /** What to actually render in an &lt;img&gt; — the uploaded image's URL when present, else the manual URL below. */
        String logoUrl,
        /** The manually-entered URL, if any; distinct from logoUrl so the settings form doesn't echo back the uploaded-image URL as if it were user input. */
        String logoUrlRaw,
        boolean logoUploaded,
        ThemeColor themeColor,
        String tagline
) {
    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getSlug(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.isActive(),
                restaurant.isVegOnly(),
                resolveLogoUrl(restaurant),
                restaurant.getLogoUrl(),
                restaurant.getLogoImage() != null,
                restaurant.getThemeColor(),
                restaurant.getTagline()
        );
    }

    /** Absolute URL to the uploaded logo image when one exists, else the restaurant's manually-entered URL (possibly null). */
    public static String resolveLogoUrl(Restaurant restaurant) {
        if (restaurant.getLogoImage() != null) {
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/public/restaurants/{id}/logo")
                    .buildAndExpand(restaurant.getId())
                    .toUriString();
        }
        return restaurant.getLogoUrl();
    }
}
