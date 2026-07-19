package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookTableRequest(

        @NotBlank
        @Size(max = 100)
        String guestName,

        @NotBlank
        @Size(max = 20)
        String guestPhone,

        @NotNull
        @Min(1)
        Integer partySize
) {
}
