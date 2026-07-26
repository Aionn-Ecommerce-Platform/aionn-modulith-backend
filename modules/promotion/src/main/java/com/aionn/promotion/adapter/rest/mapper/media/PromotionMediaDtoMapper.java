package com.aionn.promotion.adapter.rest.mapper.media;

import com.aionn.promotion.adapter.rest.dto.media.response.UploadSignatureResponse;
import com.aionn.promotion.application.dto.media.result.UploadSignatureResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromotionMediaDtoMapper {

    UploadSignatureResponse toUploadSignatureResponse(UploadSignatureResult result);
}
