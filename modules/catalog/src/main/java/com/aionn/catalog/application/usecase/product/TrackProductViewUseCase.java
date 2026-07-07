package com.aionn.catalog.application.usecase.product;

import com.aionn.catalog.application.dto.product.command.TrackProductViewCommand;
import com.aionn.catalog.application.port.in.product.TrackProductViewInputPort;
import com.aionn.catalog.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackProductViewUseCase implements TrackProductViewInputPort {

    private final ProductService productService;

    @Override
    public void execute(TrackProductViewCommand command) {
        productService.trackProductView(command.productId(), command.userId());
    }
}
