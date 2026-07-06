package com.aionn.catalog.application.port.in.product;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.query.ListProductsByMerchantQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;

public interface ListProductsByMerchantInputPort {
    PageResult<ProductResult> execute(ListProductsByMerchantQuery query);
}
