package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.query.GetRelatedProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;

import java.util.List;

public interface GetRelatedProductsInputPort {

    List<ProductResult> execute(GetRelatedProductsQuery query);
}
