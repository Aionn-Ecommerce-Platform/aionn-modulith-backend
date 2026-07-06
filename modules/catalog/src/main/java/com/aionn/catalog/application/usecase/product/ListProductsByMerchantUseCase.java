package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.query.ListProductsByMerchantQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.ListProductsByMerchantInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProductsByMerchantUseCase implements ListProductsByMerchantInputPort {

    private final ProductService productService;

    @Override
    public PageResult<ProductResult> execute(ListProductsByMerchantQuery query) {
        return productService.listByMerchant(query.merchantId(), query.pagination());
    }
}
