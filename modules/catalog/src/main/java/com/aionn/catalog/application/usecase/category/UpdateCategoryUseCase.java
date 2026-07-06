package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.command.UpdateCategoryCommand;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.port.in.category.UpdateCategoryInputPort;
import com.aionn.catalog.application.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateCategoryUseCase implements UpdateCategoryInputPort {

    private final CategoryService categoryService;

    @Override
    public CategoryResult execute(UpdateCategoryCommand command) {
        return categoryService.update(command);
    }
}
