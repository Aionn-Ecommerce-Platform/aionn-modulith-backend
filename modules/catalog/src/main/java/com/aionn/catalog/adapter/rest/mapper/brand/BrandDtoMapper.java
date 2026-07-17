package com.aionn.catalog.adapter.rest.mapper.brand;

import com.aionn.catalog.adapter.rest.dto.brand.request.CreateBrandRequest;
import com.aionn.catalog.adapter.rest.dto.brand.request.DeleteBrandRequest;
import com.aionn.catalog.adapter.rest.dto.brand.request.UpdateBrandRequest;
import com.aionn.catalog.adapter.rest.dto.brand.response.BrandResponse;
import com.aionn.catalog.application.dto.brand.command.CreateBrandCommand;
import com.aionn.catalog.application.dto.brand.command.DeleteBrandCommand;
import com.aionn.catalog.application.dto.brand.command.UpdateBrandCommand;
import com.aionn.catalog.application.dto.brand.result.BrandResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrandDtoMapper {

    CreateBrandCommand toCreateBrandCommand(CreateBrandRequest request);

    UpdateBrandCommand toUpdateBrandCommand(String brandId, UpdateBrandRequest request);

    DeleteBrandCommand toDeleteBrandCommand(String brandId, DeleteBrandRequest request);

    BrandResponse toResponse(BrandResult result);

    List<BrandResponse> toResponses(List<BrandResult> results);
}
