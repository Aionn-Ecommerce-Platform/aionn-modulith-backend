package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.command.MoveCategoryCommand;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.mapper.CategoryResultMapper;
import com.aionn.catalog.application.port.in.category.MoveCategoryInputPort;
import com.aionn.catalog.application.service.CategoryService;
import com.aionn.catalog.domain.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoveCategoryUseCase implements MoveCategoryInputPort {

    private final CategoryService categoryService;
    private final CategoryResultMapper categoryResultMapper;

    @Override
    @Transactional
    public CategoryResult execute(MoveCategoryCommand command) {
        Category category = categoryService.move(command);
        return categoryResultMapper.toResult(category);
    }
}
