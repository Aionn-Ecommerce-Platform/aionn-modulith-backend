package com.aionn.catalog.application.port.in.category;

import com.aionn.catalog.application.dto.category.command.MoveCategoryCommand;
import com.aionn.catalog.application.dto.category.result.CategoryResult;

public interface MoveCategoryInputPort {

    CategoryResult execute(MoveCategoryCommand command);
}
