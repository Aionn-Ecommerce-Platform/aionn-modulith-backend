package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.query.SearchProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface SearchProductsInputPort {
    PageResult<ProductResult> execute(SearchProductsQuery query);
}
