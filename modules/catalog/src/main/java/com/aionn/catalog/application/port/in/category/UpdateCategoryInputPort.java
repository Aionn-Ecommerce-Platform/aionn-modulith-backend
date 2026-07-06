package com.aionn.catalog.application.port.in.category;

import com.aionn.catalog.application.dto.category.command.UpdateCategoryCommand;
import com.aionn.catalog.application.dto.category.result.CategoryResult;

public interface UpdateCategoryInputPort {

    CategoryResult execute(UpdateCategoryCommand command);
}
