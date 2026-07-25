package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.query.GetCategoryQuery;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.mapper.CategoryResultMapper;
import com.aionn.catalog.application.port.in.category.GetCategoryInputPort;
import com.aionn.catalog.application.service.CategoryService;
import com.aionn.catalog.domain.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCategoryUseCase implements GetCategoryInputPort {

    private final CategoryService categoryService;
    private final CategoryResultMapper categoryResultMapper;

    @Override
    @Transactional(readOnly = true)
    public CategoryResult execute(GetCategoryQuery query) {
        Category category = categoryService.get(query.categoryId());
        return categoryResultMapper.toResult(category);
    }
}
