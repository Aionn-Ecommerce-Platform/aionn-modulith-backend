package com.aionn.catalog.application.port.in.category;

import com.aionn.catalog.application.dto.category.command.CreateCategoryCommand;
import com.aionn.catalog.application.dto.category.result.CategoryResult;

public interface CreateCategoryInputPort {

    CategoryResult execute(CreateCategoryCommand command);
}
