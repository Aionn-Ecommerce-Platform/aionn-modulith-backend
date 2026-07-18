package com.aionn.ordering.adapter.rest.dto.request;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(@jakarta.validation.constraints.NotNull @Min(0) Integer newQty) {
}

