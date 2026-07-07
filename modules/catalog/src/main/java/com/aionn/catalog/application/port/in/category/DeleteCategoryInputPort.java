package com.aionn.catalog.application.port.in.category;

import com.aionn.catalog.application.dto.category.command.DeleteCategoryCommand;

public interface DeleteCategoryInputPort {

    void execute(DeleteCategoryCommand command);
}
