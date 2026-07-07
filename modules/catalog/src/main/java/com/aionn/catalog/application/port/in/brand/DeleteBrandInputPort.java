package com.aionn.catalog.application.port.in.brand;

import com.aionn.catalog.application.dto.brand.command.DeleteBrandCommand;

public interface DeleteBrandInputPort {

    void execute(DeleteBrandCommand command);
}
