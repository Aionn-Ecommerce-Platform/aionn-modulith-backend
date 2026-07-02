package com.aionn.identity.adapter.rest.mapper.media;

import com.aionn.identity.adapter.rest.dto.media.response.UploadSignatureResponse;
import com.aionn.identity.application.dto.media.result.UploadSignatureResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MediaDtoMapper {

    UploadSignatureResponse toUploadSignatureResponse(UploadSignatureResult result);
}
