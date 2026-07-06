package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.command.MoveCategoryCommand;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.port.in.category.MoveCategoryInputPort;
import com.aionn.catalog.application.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoveCategoryUseCase implements MoveCategoryInputPort {

    private final CategoryService categoryService;

    @Override
    public CategoryResult execute(MoveCategoryCommand command) {
        return categoryService.move(command);
    }
}
