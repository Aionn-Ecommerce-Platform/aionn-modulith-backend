package com.aionn.ordering.adapter.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmShippedRequest(@NotBlank String shipmentId) {
}

