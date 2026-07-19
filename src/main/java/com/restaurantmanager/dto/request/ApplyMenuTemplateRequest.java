package com.restaurantmanager.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ApplyMenuTemplateRequest(

        @NotEmpty(message = "Select at least one item to add")
        List<String> itemKeys
) {
}
