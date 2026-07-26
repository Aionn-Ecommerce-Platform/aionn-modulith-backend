package com.aionn.catalog.application.usecase.brand;

import com.aionn.catalog.application.dto.brand.command.UpdateBrandCommand;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.mapper.BrandResultMapper;
import com.aionn.catalog.application.port.in.brand.UpdateBrandInputPort;
import com.aionn.catalog.application.service.BrandService;
import com.aionn.catalog.domain.model.Brand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateBrandUseCase implements UpdateBrandInputPort {

    private final BrandService brandService;
    private final BrandResultMapper brandResultMapper;

    @Override
    @Transactional
    public BrandResult execute(UpdateBrandCommand command) {
        Brand brand = brandService.update(command);
        return brandResultMapper.toResult(brand);
    }
}
