package com.aionn.ordering.adapter.rest.dto.request;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(@NotNull @Min(0) Integer newQty) {
}

