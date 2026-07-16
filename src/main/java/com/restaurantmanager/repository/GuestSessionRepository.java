package com.restaurantmanager.repository;

import com.restaurantmanager.entity.GuestSession;
import com.restaurantmanager.entity.GuestSessionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuestSessionRepository extends JpaRepository<GuestSession, UUID> {

    @EntityGraph(attributePaths = {"restaurant", "table"})
    Optional<GuestSession> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    @EntityGraph(attributePaths = {"restaurant", "table"})
    List<GuestSession> findByRestaurantIdAndStatusOrderByCreatedAtAsc(UUID restaurantId, GuestSessionStatus status);
}
