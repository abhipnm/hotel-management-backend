package com.restaurantmanager.repository;

import com.restaurantmanager.entity.Reservation;
import com.restaurantmanager.entity.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @EntityGraph(attributePaths = {"table"})
    List<Reservation> findByRestaurantIdOrderByReservationTimeAsc(UUID restaurantId);

    @EntityGraph(attributePaths = {"table"})
    List<Reservation> findByRestaurantIdAndStatusInOrderByReservationTimeAsc(UUID restaurantId, Collection<ReservationStatus> statuses);

    Optional<Reservation> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    List<Reservation> findByTableIdAndStatusIn(UUID tableId, Collection<ReservationStatus> statuses);
}
