package com.restaurantmanager.repository;

import com.restaurantmanager.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"restaurant"})
    List<AppUser> findByRestaurantIdOrderByNameAsc(UUID restaurantId);

    Optional<AppUser> findByIdAndRestaurantId(UUID id, UUID restaurantId);
}
