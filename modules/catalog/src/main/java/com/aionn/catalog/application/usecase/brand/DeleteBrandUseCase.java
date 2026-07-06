package com.aionn.catalog.application.usecase.brand;

import com.aionn.catalog.application.dto.brand.command.DeleteBrandCommand;
import com.aionn.catalog.application.port.in.brand.DeleteBrandInputPort;
import com.aionn.catalog.application.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteBrandUseCase implements DeleteBrandInputPort {

    private final BrandService brandService;

    @Override
    public void execute(DeleteBrandCommand command) {
        brandService.delete(command);
    }
}
