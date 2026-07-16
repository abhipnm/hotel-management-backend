package com.restaurantmanager.repository;

import com.restaurantmanager.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    Optional<Restaurant> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
