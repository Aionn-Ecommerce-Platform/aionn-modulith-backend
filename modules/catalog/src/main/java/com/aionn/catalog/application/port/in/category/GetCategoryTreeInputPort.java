package com.aionn.catalog.application.port.in.category;

import com.aionn.catalog.application.dto.category.result.CategoryTreeNode;

import java.util.List;

public interface GetCategoryTreeInputPort {

    List<CategoryTreeNode> execute();
}
