package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignReservationTableRequest(
        @NotNull
        UUID tableId
) {
}
