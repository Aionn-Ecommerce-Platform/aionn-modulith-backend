package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.command.CreateCategoryCommand;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.port.in.category.CreateCategoryInputPort;
import com.aionn.catalog.application.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateCategoryUseCase implements CreateCategoryInputPort {

    private final CategoryService categoryService;

    @Override
    public CategoryResult execute(CreateCategoryCommand command) {
        return categoryService.create(command);
    }
}
