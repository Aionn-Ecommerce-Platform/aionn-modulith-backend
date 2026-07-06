package com.aionn.catalog.application.port.in.category;

import com.aionn.catalog.application.dto.category.query.GetCategoryQuery;
import com.aionn.catalog.application.dto.category.result.CategoryResult;

public interface GetCategoryInputPort {

    CategoryResult execute(GetCategoryQuery query);
}
