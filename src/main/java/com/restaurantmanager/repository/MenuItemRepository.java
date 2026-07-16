package com.restaurantmanager.repository;

import com.restaurantmanager.entity.FoodType;
import com.restaurantmanager.entity.MenuItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    boolean existsByRestaurantIdAndFoodTypeNot(UUID restaurantId, FoodType foodType);

    @EntityGraph(attributePaths = {"category"})
    List<MenuItem> findByRestaurantIdOrderByDisplayOrderAsc(UUID restaurantId);

    @EntityGraph(attributePaths = {"category"})
    List<MenuItem> findByRestaurantIdAndAvailableTrueOrderByDisplayOrderAsc(UUID restaurantId);

    @EntityGraph(attributePaths = {"category"})
    List<MenuItem> findByCategoryIdOrderByDisplayOrderAsc(UUID categoryId);

    @EntityGraph(attributePaths = {"category"})
    Optional<MenuItem> findByIdAndRestaurantId(UUID id, UUID restaurantId);
}
