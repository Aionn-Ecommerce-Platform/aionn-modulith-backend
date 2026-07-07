package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.product.query.GetPopularProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;

import java.util.List;

public interface GetPopularProductsInputPort {

    List<ProductResult> execute(GetPopularProductsQuery query);
}
