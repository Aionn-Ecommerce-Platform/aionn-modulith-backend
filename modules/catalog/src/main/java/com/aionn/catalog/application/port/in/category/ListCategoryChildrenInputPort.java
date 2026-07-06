package com.aionn.catalog.application.port.in.category;

import com.aionn.catalog.application.dto.category.query.ListCategoryChildrenQuery;
import com.aionn.catalog.application.dto.category.result.CategoryResult;

import java.util.List;

public interface ListCategoryChildrenInputPort {

    List<CategoryResult> execute(ListCategoryChildrenQuery query);
}
