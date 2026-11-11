package com.aionn.catalog.adapter.rest.dto.product.request;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateAiMetadataRequest(
        List<@NotBlank String> tags,
        @Size(max = 5000) String aiDescription) {
}
