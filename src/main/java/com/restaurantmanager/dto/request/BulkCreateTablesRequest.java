package com.restaurantmanager.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkCreateTablesRequest(

        @NotEmpty
        @Size(max = 100)
        @Valid
        List<CreateTableRequest> tables
) {
}
