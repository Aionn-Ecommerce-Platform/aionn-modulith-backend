package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.PublishProductCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.port.in.product.PublishProductInputPort;
import com.aionn.catalog.application.service.ProductService;
import com.aionn.catalog.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublishProductUseCase implements PublishProductInputPort {

    private final ProductService productService;
    private final ProductResultMapper productResultMapper;

    @Override
    @Transactional
    public ProductResult execute(PublishProductCommand command) {
        Product product = productService.publish(command);
        return productResultMapper.toResult(product);
    }
}
