package com.aionn.catalog.application.dto.product.query;

import com.aionn.sharedkernel.application.query.Query;

import java.util.List;

public record GetPersonalizedProductsQuery(
        String userId,
        List<String> categoryIds,
        List<String> brandIds,
        int limit) implements Query {
}
