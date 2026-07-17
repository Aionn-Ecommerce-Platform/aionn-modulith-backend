package com.aionn.catalog.adapter.rest.dto.category.response;

import java.util.List;

public record CategoryTreeNodeResponse(
        CategoryResponse category,
        List<CategoryTreeNodeResponse> children) {
}
