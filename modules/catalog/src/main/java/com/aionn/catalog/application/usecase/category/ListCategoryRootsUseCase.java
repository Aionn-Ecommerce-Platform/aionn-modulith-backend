package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.mapper.CategoryResultMapper;
import com.aionn.catalog.application.port.in.category.ListCategoryRootsInputPort;
import com.aionn.catalog.application.service.CategoryService;
import com.aionn.catalog.domain.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListCategoryRootsUseCase implements ListCategoryRootsInputPort {

    private final CategoryService categoryService;
    private final CategoryResultMapper categoryResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResult> execute() {
        List<Category> categories = categoryService.listRoots();
        return categoryResultMapper.toResults(categories);
    }
}
