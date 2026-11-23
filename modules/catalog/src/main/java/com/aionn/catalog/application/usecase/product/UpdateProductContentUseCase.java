package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.UpdateProductContentCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.mapper.ProductResultMapper;
import com.aionn.catalog.application.port.in.product.UpdateProductContentInputPort;
import com.aionn.catalog.application.service.ProductService;
import com.aionn.catalog.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProductContentUseCase implements UpdateProductContentInputPort {
    private final ProductService productService;
    private final ProductResultMapper productResultMapper;

    @Override
    @Transactional
    public ProductResult execute(UpdateProductContentCommand command) {
        Product product = productService.updateContent(command);
        return productResultMapper.toResult(product);
    }
}
