package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.query.ListCategoryChildrenQuery;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.port.in.category.ListCategoryChildrenInputPort;
import com.aionn.catalog.application.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListCategoryChildrenUseCase implements ListCategoryChildrenInputPort {

    private final CategoryService categoryService;

    @Override
    public List<CategoryResult> execute(ListCategoryChildrenQuery query) {
        return categoryService.listChildren(query.parentId());
    }
}
