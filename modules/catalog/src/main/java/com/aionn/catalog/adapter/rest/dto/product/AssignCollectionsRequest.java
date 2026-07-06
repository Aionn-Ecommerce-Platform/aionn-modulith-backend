package com.aionn.catalog.adapter.rest.dto.product;

import java.util.List;

public record AssignCollectionsRequest(
        List<@jakarta.validation.constraints.NotBlank String> collectionIds) {
}
