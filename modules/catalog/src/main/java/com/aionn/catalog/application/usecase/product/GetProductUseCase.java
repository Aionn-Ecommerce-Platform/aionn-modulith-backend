package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.query.GetProductQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.port.in.product.GetProductInputPort;
import com.aionn.catalog.application.service.ProductService;
import com.aionn.catalog.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProductUseCase implements GetProductInputPort {

    private final ProductService productService;
    private final ProductResultMapper productResultMapper;

    @Override
    @Transactional(readOnly = true)
    public ProductResult execute(GetProductQuery query) {
        Product product = productService.get(query.productId());
        return productResultMapper.toResult(product);
    }
}
