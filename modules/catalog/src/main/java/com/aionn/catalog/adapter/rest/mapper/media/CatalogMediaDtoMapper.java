package com.aionn.catalog.adapter.rest.mapper.media;

import com.aionn.catalog.adapter.rest.dto.media.response.UploadSignatureResponse;
import com.aionn.catalog.application.dto.media.result.UploadSignatureResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CatalogMediaDtoMapper {

    UploadSignatureResponse toUploadSignatureResponse(UploadSignatureResult result);
}
