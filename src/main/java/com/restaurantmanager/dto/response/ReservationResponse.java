package com.restaurantmanager.dto.response;

import com.restaurantmanager.entity.Reservation;
import com.restaurantmanager.entity.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID tableId,
        String tableNumber,
        String guestName,
        String guestPhone,
        int partySize,
        Instant reservationTime,
        ReservationStatus status,
        String notes,
        Instant createdAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getTable() != null ? reservation.getTable().getId() : null,
                reservation.getTable() != null ? reservation.getTable().getTableNumber() : null,
                reservation.getGuestName(),
                reservation.getGuestPhone(),
                reservation.getPartySize(),
                reservation.getReservationTime(),
                reservation.getStatus(),
                reservation.getNotes(),
                reservation.getCreatedAt()
        );
    }
}
