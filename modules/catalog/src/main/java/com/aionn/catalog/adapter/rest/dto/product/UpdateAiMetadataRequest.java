package com.aionn.catalog.adapter.rest.dto.product;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateAiMetadataRequest(
        List<@jakarta.validation.constraints.NotBlank String> tags,
        @Size(max = 5000) String aiDescription) {
}
