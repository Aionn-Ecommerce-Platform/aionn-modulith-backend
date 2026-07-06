package com.aionn.catalog.application.dto.category.result;

import java.util.List;

public record CategoryTreeNode(
        CategoryResult category,
        List<CategoryTreeNode> children) {
}
