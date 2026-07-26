package com.aionn.catalog.application.usecase.category;

import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.dto.category.result.CategoryTreeNode;
import com.aionn.catalog.application.mapper.CategoryResultMapper;
import com.aionn.catalog.application.port.in.category.GetCategoryTreeInputPort;
import com.aionn.catalog.application.service.CategoryService;
import com.aionn.catalog.domain.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetCategoryTreeUseCase implements GetCategoryTreeInputPort {

    private final CategoryService categoryService;
    private final CategoryResultMapper categoryResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeNode> execute() {
        List<Category> categories = categoryService.getTree();
        List<CategoryResult> results = categoryResultMapper.toResults(categories);

        Map<String, List<CategoryResult>> byParent = results.stream()
                .filter(c -> c.parentId() != null)
                .collect(Collectors.groupingBy(CategoryResult::parentId));

        List<CategoryResult> roots = results.stream()
                .filter(c -> c.parentId() == null)
                .toList();

        return roots.stream()
                .map(root -> buildNode(root, byParent))
                .toList();
    }

    private CategoryTreeNode buildNode(CategoryResult category, Map<String, List<CategoryResult>> byParent) {
        List<CategoryResult> children = byParent.getOrDefault(category.categoryId(), List.of());
        List<CategoryTreeNode> childNodes = children.stream()
                .map(child -> buildNode(child, byParent))
                .toList();
        return new CategoryTreeNode(category, childNodes);
    }
}
