package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTableRequest(

        @NotBlank
        @Size(max = 20)
        String tableNumber,

        @NotNull
        Boolean active
) {
}
