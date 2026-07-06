package com.aionn.catalog.adapter.rest.mapper.brand;

import com.aionn.catalog.adapter.rest.dto.brand.CreateBrandRequest;
import com.aionn.catalog.adapter.rest.dto.brand.DeleteBrandRequest;
import com.aionn.catalog.adapter.rest.dto.brand.UpdateBrandRequest;
import com.aionn.catalog.application.dto.brand.command.CreateBrandCommand;
import com.aionn.catalog.application.dto.brand.command.DeleteBrandCommand;
import com.aionn.catalog.application.dto.brand.command.UpdateBrandCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BrandDtoMapper {

    CreateBrandCommand toCreateBrandCommand(CreateBrandRequest request);

    UpdateBrandCommand toUpdateBrandCommand(String brandId, UpdateBrandRequest request);

    DeleteBrandCommand toDeleteBrandCommand(String brandId, DeleteBrandRequest request);
}
