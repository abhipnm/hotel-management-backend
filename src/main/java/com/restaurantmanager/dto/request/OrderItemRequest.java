package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OrderItemRequest(

        @NotNull
        UUID menuItemId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        @Size(max = 300)
        String notes
) {
}
