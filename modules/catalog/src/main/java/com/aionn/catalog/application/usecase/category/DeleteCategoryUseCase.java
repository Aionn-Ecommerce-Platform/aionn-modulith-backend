package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.command.DeleteCategoryCommand;
import com.aionn.catalog.application.port.in.category.DeleteCategoryInputPort;
import com.aionn.catalog.application.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteCategoryUseCase implements DeleteCategoryInputPort {

    private final CategoryService categoryService;

    @Override
    public void execute(DeleteCategoryCommand command) {
        categoryService.delete(command.categoryId());
    }
}
