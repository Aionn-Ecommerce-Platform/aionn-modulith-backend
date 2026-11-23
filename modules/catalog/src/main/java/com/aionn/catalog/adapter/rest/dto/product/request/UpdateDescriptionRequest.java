package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.constraints.Size;

public record UpdateDescriptionRequest(
        @Size(max = 10000) String description) {
}
