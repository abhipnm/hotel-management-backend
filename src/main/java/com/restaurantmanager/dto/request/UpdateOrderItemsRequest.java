package com.restaurantmanager.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateOrderItemsRequest(

        @NotEmpty(message = "At least one item is required")
        @Valid
        List<OrderItemRequest> items,

        @Size(max = 500)
        String notes
) {
}
