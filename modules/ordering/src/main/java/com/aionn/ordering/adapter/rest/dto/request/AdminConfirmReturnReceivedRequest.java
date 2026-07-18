package com.aionn.ordering.adapter.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminConfirmReturnReceivedRequest(
        @NotBlank @Size(max = 200) String itemCondition) {
}
