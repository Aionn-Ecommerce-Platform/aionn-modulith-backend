package com.aionn.catalog.adapter.rest.dto.product.request;

import java.util.List;

public record UpdateMediaRequest(List<@jakarta.validation.constraints.NotBlank String> images) {
}
