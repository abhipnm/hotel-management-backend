package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateReservationRequest(

        @NotBlank
        @Size(max = 100)
        String guestName,

        @NotBlank
        @Size(max = 20)
        String guestPhone,

        @NotNull
        @Min(1)
        Integer partySize,

        @NotNull
        @Future
        Instant reservationTime,

        /** Optional — a table can be assigned later when confirming. */
        UUID tableId,

        @Size(max = 500)
        String notes
) {
}
