package com.aionn.catalog.adapter.rest.mapper.category;

import com.aionn.catalog.adapter.rest.dto.category.request.CreateCategoryRequest;
import com.aionn.catalog.adapter.rest.dto.category.request.MoveCategoryRequest;
import com.aionn.catalog.adapter.rest.dto.category.request.UpdateCategoryRequest;
import com.aionn.catalog.adapter.rest.dto.category.response.CategoryResponse;
import com.aionn.catalog.adapter.rest.dto.category.response.CategoryTreeNodeResponse;
import com.aionn.catalog.application.dto.category.command.CreateCategoryCommand;
import com.aionn.catalog.application.dto.category.command.MoveCategoryCommand;
import com.aionn.catalog.application.dto.category.command.UpdateCategoryCommand;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.dto.category.result.CategoryTreeNode;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryDtoMapper {

    CreateCategoryCommand toCreateCategoryCommand(CreateCategoryRequest request);

    UpdateCategoryCommand toUpdateCategoryCommand(String categoryId, UpdateCategoryRequest request);

    MoveCategoryCommand toMoveCategoryCommand(String categoryId, MoveCategoryRequest request);

    CategoryResponse toResponse(CategoryResult result);

    List<CategoryResponse> toResponses(List<CategoryResult> results);

    CategoryTreeNodeResponse toTreeNodeResponse(CategoryTreeNode node);

    List<CategoryTreeNodeResponse> toTreeNodeResponses(List<CategoryTreeNode> nodes);
}
