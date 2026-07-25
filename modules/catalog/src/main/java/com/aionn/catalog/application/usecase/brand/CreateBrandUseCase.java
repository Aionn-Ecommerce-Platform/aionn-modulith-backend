package com.aionn.catalog.application.usecase.brand;

import com.aionn.catalog.application.dto.brand.command.CreateBrandCommand;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.mapper.BrandResultMapper;
import com.aionn.catalog.application.port.in.brand.CreateBrandInputPort;
import com.aionn.catalog.application.service.BrandService;
import com.aionn.catalog.domain.model.Brand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateBrandUseCase implements CreateBrandInputPort {

    private final BrandService brandService;
    private final BrandResultMapper brandResultMapper;

    @Override
    @Transactional
    public BrandResult execute(CreateBrandCommand command) {
        Brand brand = brandService.create(command);
        return brandResultMapper.toResult(brand);
    }
}
