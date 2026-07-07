package com.aionn.catalog.application.usecase.brand;

import com.aionn.catalog.application.dto.brand.command.CreateBrandCommand;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.port.in.brand.CreateBrandInputPort;
import com.aionn.catalog.application.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateBrandUseCase implements CreateBrandInputPort {

    private final BrandService brandService;

    @Override
    public BrandResult execute(CreateBrandCommand command) {
        return brandService.create(command);
    }
}
