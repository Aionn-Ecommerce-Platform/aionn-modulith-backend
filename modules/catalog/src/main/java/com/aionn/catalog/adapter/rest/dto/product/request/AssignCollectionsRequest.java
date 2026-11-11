package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AssignCollectionsRequest(
        List<@NotBlank String> collectionIds) {
}
