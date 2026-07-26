package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.command.UpdateCategoryCommand;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.mapper.CategoryResultMapper;
import com.aionn.catalog.application.port.in.category.UpdateCategoryInputPort;
import com.aionn.catalog.application.service.CategoryService;
import com.aionn.catalog.domain.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCategoryUseCase implements UpdateCategoryInputPort {

    private final CategoryService categoryService;
    private final CategoryResultMapper categoryResultMapper;

    @Override
    @Transactional
    public CategoryResult execute(UpdateCategoryCommand command) {
        Category category = categoryService.update(command);
        return categoryResultMapper.toResult(category);
    }
}
