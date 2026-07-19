package com.restaurantmanager.repository;

import com.restaurantmanager.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

    List<MenuCategory> findByRestaurantIdOrderByDisplayOrderAsc(UUID restaurantId);

    List<MenuCategory> findByRestaurantIdAndActiveTrueOrderByDisplayOrderAsc(UUID restaurantId);

    Optional<MenuCategory> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    Optional<MenuCategory> findByRestaurantIdAndNameIgnoreCase(UUID restaurantId, String name);

    int countByRestaurantId(UUID restaurantId);
}
