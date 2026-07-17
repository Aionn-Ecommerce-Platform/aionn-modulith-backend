package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectProductRequest(
        @NotBlank @Size(max = 50) String reasonCode,
        @Size(max = 2000) String feedback) {
}
