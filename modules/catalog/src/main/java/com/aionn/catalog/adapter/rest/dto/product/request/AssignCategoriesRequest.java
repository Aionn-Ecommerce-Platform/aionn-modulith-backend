package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AssignCategoriesRequest(@NotEmpty List<@NotBlank String> categoryIds) {
}
