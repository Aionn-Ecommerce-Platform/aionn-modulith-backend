package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.query.GetCategoryQuery;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.port.in.category.GetCategoryInputPort;
import com.aionn.catalog.application.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCategoryUseCase implements GetCategoryInputPort {

    private final CategoryService categoryService;

    @Override
    public CategoryResult execute(GetCategoryQuery query) {
        return categoryService.get(query.categoryId());
    }
}
