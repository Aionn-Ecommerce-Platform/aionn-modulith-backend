package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.DefineAttributesCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.port.in.product.DefineAttributesInputPort;
import com.aionn.catalog.application.service.ProductService;
import com.aionn.catalog.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefineAttributesUseCase implements DefineAttributesInputPort {

    private final ProductService productService;
    private final ProductResultMapper productResultMapper;

    @Override
    @Transactional
    public ProductResult execute(DefineAttributesCommand command) {
        Product product = productService.defineAttributes(command);
        return productResultMapper.toResult(product);
    }
}
