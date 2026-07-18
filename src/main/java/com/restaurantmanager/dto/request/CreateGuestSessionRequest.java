package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGuestSessionRequest(

        @NotBlank(message = "QR token is required")
        String qrToken,

        @NotBlank(message = "Guest name is required")
        @Size(max = 100)
        String guestName,

        /** Optional — providing it lets us recognize the guest on a future visit. */
        @Size(max = 20)
        String guestPhone
) {
}
