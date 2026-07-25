package com.aionn.catalog.application.usecase.brand;

import com.aionn.catalog.application.dto.brand.query.ListBrandsQuery;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.mapper.BrandResultMapper;
import com.aionn.catalog.application.port.in.brand.ListBrandsInputPort;
import com.aionn.catalog.application.service.BrandService;
import com.aionn.catalog.domain.model.Brand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListBrandsUseCase implements ListBrandsInputPort {

    private final BrandService brandService;
    private final BrandResultMapper brandResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<BrandResult> execute(ListBrandsQuery query) {
        PageResult<Brand> page = brandService.list(query.pagination());
        return brandResultMapper.toPageResult(page);
    }
}
