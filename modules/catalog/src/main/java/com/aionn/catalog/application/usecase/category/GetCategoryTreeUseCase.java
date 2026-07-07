package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.result.CategoryTreeNode;
import com.aionn.catalog.application.port.in.category.GetCategoryTreeInputPort;
import com.aionn.catalog.application.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCategoryTreeUseCase implements GetCategoryTreeInputPort {

    private final CategoryService categoryService;

    @Override
    public List<CategoryTreeNode> execute() {
        return categoryService.getTree();
    }
}
