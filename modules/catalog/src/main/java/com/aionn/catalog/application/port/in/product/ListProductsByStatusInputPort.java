package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.query.ListProductsByStatusQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface ListProductsByStatusInputPort {
    PageResult<ProductResult> execute(ListProductsByStatusQuery query);
}
