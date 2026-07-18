package com.aionn.ordering.adapter.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRejectReturnRequest(
        @NotBlank @Size(max = 500) String reason) {
}
