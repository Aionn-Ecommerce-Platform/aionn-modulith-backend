package com.aionn.ordering.adapter.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmItemReceivedRequest(@NotBlank @Size(max = 500) String itemCondition) {
}

