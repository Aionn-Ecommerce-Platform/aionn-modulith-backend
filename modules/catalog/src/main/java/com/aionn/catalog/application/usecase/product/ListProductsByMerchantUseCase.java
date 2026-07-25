package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.product.query.ListProductsByMerchantQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.port.in.product.ListProductsByMerchantInputPort;
import com.aionn.catalog.application.service.ProductService;
import com.aionn.catalog.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListProductsByMerchantUseCase implements ListProductsByMerchantInputPort {

    private final ProductService productService;
    private final ProductResultMapper productResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductResult> execute(ListProductsByMerchantQuery query) {
        PageResult<Product> page = productService.listByMerchant(query.merchantId(), query.pagination());
        return productResultMapper.toPageResult(page);
    }
}
