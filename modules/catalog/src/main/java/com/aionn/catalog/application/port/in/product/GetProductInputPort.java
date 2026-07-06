package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.query.GetProductQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface GetProductInputPort {
    ProductResult execute(GetProductQuery query);
}
