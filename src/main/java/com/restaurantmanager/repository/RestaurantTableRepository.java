package com.restaurantmanager.repository;

import com.restaurantmanager.entity.RestaurantTable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {

    @EntityGraph(attributePaths = {"restaurant"})
    Optional<RestaurantTable> findByQrToken(String qrToken);

    List<RestaurantTable> findByRestaurantIdOrderByTableNumberAsc(UUID restaurantId);

    Optional<RestaurantTable> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    boolean existsByRestaurantIdAndTableNumberIgnoreCase(UUID restaurantId, String tableNumber);

    List<RestaurantTable> findByAssignedWaiterId(UUID waiterId);
}
