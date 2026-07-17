package com.aionn.catalog.adapter.rest.dto.product.request;

import java.util.Map;

public record DefineAttributesRequest(Map<String, String> attributes) {
}
