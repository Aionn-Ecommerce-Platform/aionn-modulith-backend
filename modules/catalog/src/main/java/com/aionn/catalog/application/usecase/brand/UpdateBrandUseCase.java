package com.aionn.catalog.application.usecase.brand;

import com.aionn.catalog.application.dto.brand.command.UpdateBrandCommand;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.port.in.brand.UpdateBrandInputPort;
import com.aionn.catalog.application.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateBrandUseCase implements UpdateBrandInputPort {

    private final BrandService brandService;

    @Override
    public BrandResult execute(UpdateBrandCommand command) {
        return brandService.update(command);
    }
}
