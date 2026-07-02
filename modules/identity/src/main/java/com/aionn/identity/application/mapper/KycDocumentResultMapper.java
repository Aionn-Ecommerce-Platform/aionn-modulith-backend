package com.aionn.identity.application.mapper;

import com.aionn.identity.application.dto.kyc.result.KycDocumentResult;
import com.aionn.identity.domain.model.KycDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KycDocumentResultMapper {

    @Mapping(target = "type", expression = "java(domain.getType().name())")
    @Mapping(target = "status", expression = "java(domain.getStatus().name())")
    KycDocumentResult toResult(KycDocument domain);
}
