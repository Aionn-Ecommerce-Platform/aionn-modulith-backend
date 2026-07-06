package com.aionn.catalog.application.port.in.brand;

import com.aionn.catalog.application.dto.brand.query.ListBrandsQuery;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import com.aionn.catalog.application.dto.common.PageResult;

public interface ListBrandsInputPort {

    PageResult<BrandResult> execute(ListBrandsQuery query);
}
