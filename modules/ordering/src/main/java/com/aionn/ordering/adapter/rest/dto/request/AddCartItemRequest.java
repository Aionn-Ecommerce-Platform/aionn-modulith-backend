package com.aionn.ordering.adapter.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(@NotBlank String skuId, @Positive int qty) {
}

