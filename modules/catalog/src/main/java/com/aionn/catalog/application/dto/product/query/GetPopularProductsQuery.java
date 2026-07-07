package com.aionn.catalog.application.dto.product.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetPopularProductsQuery(int limit) implements Query {
}
