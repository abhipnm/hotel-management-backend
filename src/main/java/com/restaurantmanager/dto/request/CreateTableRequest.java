package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTableRequest(

        @NotBlank
        @Size(max = 20)
        String tableNumber
) {
}
