package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.SubmitForReviewCommand;
import com.aionn.catalog.application.dto.product.result.ProductResult;
import com.aionn.catalog.application.port.in.product.SubmitForReviewInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmitForReviewUseCase implements SubmitForReviewInputPort {

    private final ProductService productService;

    @Override
    public ProductResult execute(SubmitForReviewCommand command) {
        return productService.submitForReview(command);
    }
}
