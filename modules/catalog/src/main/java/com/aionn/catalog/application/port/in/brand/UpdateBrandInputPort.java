package com.aionn.catalog.application.port.in.brand;

import com.aionn.catalog.application.dto.brand.command.UpdateBrandCommand;
import com.aionn.catalog.application.dto.brand.result.BrandResult;

public interface UpdateBrandInputPort {

    BrandResult execute(UpdateBrandCommand command);
}
