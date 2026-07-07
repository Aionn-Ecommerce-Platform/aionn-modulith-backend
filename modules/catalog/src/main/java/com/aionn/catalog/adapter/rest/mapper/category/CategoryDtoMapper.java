package com.aionn.catalog.adapter.rest.mapper.category;

import com.aionn.catalog.adapter.rest.dto.category.CreateCategoryRequest;
import com.aionn.catalog.adapter.rest.dto.category.MoveCategoryRequest;
import com.aionn.catalog.adapter.rest.dto.category.UpdateCategoryRequest;
import com.aionn.catalog.application.dto.category.command.CreateCategoryCommand;
import com.aionn.catalog.application.dto.category.command.MoveCategoryCommand;
import com.aionn.catalog.application.dto.category.command.UpdateCategoryCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryDtoMapper {

    CreateCategoryCommand toCreateCategoryCommand(CreateCategoryRequest request);

    UpdateCategoryCommand toUpdateCategoryCommand(String categoryId, UpdateCategoryRequest request);

    MoveCategoryCommand toMoveCategoryCommand(String categoryId, MoveCategoryRequest request);
}
