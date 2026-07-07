package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.query.GetProductsBySkuIdsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;

import java.util.List;

public interface GetProductsBySkuIdsInputPort {
    List<ProductResult> execute(GetProductsBySkuIdsQuery query);
}
