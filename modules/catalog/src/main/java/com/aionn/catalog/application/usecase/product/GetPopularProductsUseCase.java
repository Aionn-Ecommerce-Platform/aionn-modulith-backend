package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.query.GetPopularProductsQuery;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.port.in.product.GetPopularProductsInputPort;
import com.aionn.catalog.application.service.ProductService;
import com.aionn.catalog.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPopularProductsUseCase implements GetPopularProductsInputPort {

    private final ProductService productService;
    private final ProductResultMapper productResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResult> execute(GetPopularProductsQuery query) {
        List<Product> products = productService.getPopularProducts(query.limit());
        return productResultMapper.toResults(products);
    }
}
