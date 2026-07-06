package com.aionn.catalog.application.dto.product.query;

import com.aionn.sharedkernel.application.query.Query;

import java.util.List;

public record GetProductsBySkuIdsQuery(List<String> skuIds) implements Query {
}
