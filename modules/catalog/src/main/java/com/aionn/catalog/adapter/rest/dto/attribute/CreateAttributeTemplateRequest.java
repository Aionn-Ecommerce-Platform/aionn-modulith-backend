package com.aionn.catalog.adapter.rest.dto.attribute;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateAttributeTemplateRequest(
        @NotBlank String categoryId,
        @NotEmpty List<String> attributeKeys) {
}

