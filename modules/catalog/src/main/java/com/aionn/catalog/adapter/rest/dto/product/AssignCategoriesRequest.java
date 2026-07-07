package com.aionn.catalog.adapter.rest.dto.product;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AssignCategoriesRequest(@NotEmpty List<@jakarta.validation.constraints.NotBlank String> categoryIds) {
}
