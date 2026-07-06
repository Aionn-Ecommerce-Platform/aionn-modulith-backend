package com.aionn.catalog.adapter.rest.dto.product;

import java.util.List;

public record UpdateMediaRequest(List<@jakarta.validation.constraints.NotBlank String> images) {
}
