package com.restaurantmanager.repository;

import com.restaurantmanager.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    Optional<Feedback> findByGuestSessionId(UUID guestSessionId);

    List<Feedback> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId);

    @Query("select coalesce(avg(f.rating), 0) from Feedback f where f.restaurant.id = :restaurantId")
    double averageRating(@Param("restaurantId") UUID restaurantId);

    long countByRestaurantId(UUID restaurantId);
}
