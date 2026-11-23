package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateMediaRequest(
        @Size(max = 12) List<@NotBlank @Size(max = 2048) String> images) {
}
