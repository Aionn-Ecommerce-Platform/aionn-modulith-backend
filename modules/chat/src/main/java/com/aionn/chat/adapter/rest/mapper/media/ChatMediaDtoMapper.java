package com.aionn.chat.adapter.rest.mapper.media;

import com.aionn.chat.adapter.rest.dto.media.response.UploadSignatureResponse;
import com.aionn.chat.application.dto.media.result.UploadSignatureResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatMediaDtoMapper {

    UploadSignatureResponse toUploadSignatureResponse(UploadSignatureResult result);
}
