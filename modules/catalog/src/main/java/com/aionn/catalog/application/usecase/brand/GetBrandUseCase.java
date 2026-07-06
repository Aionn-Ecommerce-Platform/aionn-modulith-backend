package com.aionn.catalog.application.usecase.brand;

import com.aionn.catalog.application.dto.brand.query.GetBrandQuery;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.port.in.brand.GetBrandInputPort;
import com.aionn.catalog.application.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBrandUseCase implements GetBrandInputPort {

    private final BrandService brandService;

    @Override
    public BrandResult execute(GetBrandQuery query) {
        return brandService.get(query.brandId());
    }
}
