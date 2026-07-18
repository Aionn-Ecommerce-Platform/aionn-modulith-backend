package com.aionn.ordering.adapter.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestReturnRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 2048) String evidenceUrl) {
}

