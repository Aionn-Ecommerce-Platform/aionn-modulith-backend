package com.aionn.catalog.application.port.in.brand;

import com.aionn.catalog.application.dto.brand.command.CreateBrandCommand;
import com.aionn.catalog.application.dto.brand.result.BrandResult;

public interface CreateBrandInputPort {

    BrandResult execute(CreateBrandCommand command);
}
