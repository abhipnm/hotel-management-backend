package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAnnouncementRequest(

        @NotBlank
        @Size(max = 500)
        String message
) {
}
