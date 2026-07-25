package com.aionn.catalog.application.usecase.brand;

import com.aionn.catalog.application.dto.brand.query.GetBrandQuery;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.mapper.BrandResultMapper;
import com.aionn.catalog.application.port.in.brand.GetBrandInputPort;
import com.aionn.catalog.application.service.BrandService;
import com.aionn.catalog.domain.model.Brand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetBrandUseCase implements GetBrandInputPort {

    private final BrandService brandService;
    private final BrandResultMapper brandResultMapper;

    @Override
    @Transactional(readOnly = true)
    public BrandResult execute(GetBrandQuery query) {
        Brand brand = brandService.get(query.brandId());
        return brandResultMapper.toResult(brand);
    }
}
