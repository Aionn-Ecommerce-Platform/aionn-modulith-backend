package com.aionn.catalog.application.usecase.brand;

import com.aionn.catalog.application.dto.brand.query.ListBrandsQuery;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.port.in.brand.ListBrandsInputPort;
import com.aionn.catalog.application.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListBrandsUseCase implements ListBrandsInputPort {

    private final BrandService brandService;

    @Override
    public PageResult<BrandResult> execute(ListBrandsQuery query) {
        return brandService.list(query.pagination());
    }
}
