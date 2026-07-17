package com.aionn.catalog.adapter.rest.dto.product.request;

import java.util.List;

public record AssignCollectionsRequest(
        List<@jakarta.validation.constraints.NotBlank String> collectionIds) {
}
