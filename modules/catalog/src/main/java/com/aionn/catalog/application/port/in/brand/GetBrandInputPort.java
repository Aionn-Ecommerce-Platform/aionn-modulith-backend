package com.aionn.catalog.application.port.in.brand;

import com.aionn.catalog.application.dto.brand.query.GetBrandQuery;
import com.aionn.catalog.application.dto.brand.result.BrandResult;

public interface GetBrandInputPort {

    BrandResult execute(GetBrandQuery query);
}
