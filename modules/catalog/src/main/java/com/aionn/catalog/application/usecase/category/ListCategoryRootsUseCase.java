package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.port.in.category.ListCategoryRootsInputPort;
import com.aionn.catalog.application.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListCategoryRootsUseCase implements ListCategoryRootsInputPort {

    private final CategoryService categoryService;

    @Override
    public List<CategoryResult> execute() {
        return categoryService.listRoots();
    }
}
