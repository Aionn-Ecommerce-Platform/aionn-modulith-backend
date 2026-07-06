package com.aionn.catalog.application.port.in.category;

import com.aionn.catalog.application.dto.category.result.CategoryResult;

import java.util.List;

public interface ListCategoryRootsInputPort {

    List<CategoryResult> execute();
}
