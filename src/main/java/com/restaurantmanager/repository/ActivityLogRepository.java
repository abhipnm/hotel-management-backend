package com.restaurantmanager.repository;

import com.restaurantmanager.entity.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    List<ActivityLog> findByRestaurantIdOrderByCreatedAtDesc(UUID restaurantId, Pageable pageable);
}
