package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.NotNull;

public record SetTableActiveRequest(

        @NotNull
        Boolean active
) {
}
