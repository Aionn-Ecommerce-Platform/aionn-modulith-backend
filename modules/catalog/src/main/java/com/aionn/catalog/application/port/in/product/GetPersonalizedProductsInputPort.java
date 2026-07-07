package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.query.GetPersonalizedProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;

import java.util.List;

public interface GetPersonalizedProductsInputPort {

    List<ProductResult> execute(GetPersonalizedProductsQuery query);
}
