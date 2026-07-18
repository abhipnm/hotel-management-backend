package com.restaurantmanager.dto.request;

import com.restaurantmanager.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReservationStatusRequest(
        @NotNull
        ReservationStatus status
) {
}
