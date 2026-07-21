package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTableRequest(

        @NotBlank
        @Size(max = 20)
        String tableNumber,

        @NotNull
        Boolean active,

        /** How many guests this table seats; null means unset. */
        @Min(1)
        @Max(100)
        Integer capacity
) {
}
