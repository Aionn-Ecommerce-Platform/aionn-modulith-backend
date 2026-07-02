package com.aionn.identity.infrastructure.persistence.mapper;

import com.aionn.identity.domain.model.KycDocument;
import com.aionn.identity.domain.valueobject.KycDocumentStatus;
import com.aionn.identity.domain.valueobject.KycDocumentType;
import com.aionn.identity.infrastructure.persistence.entity.KycDocumentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = { KycDocumentStatus.class, KycDocumentType.class })
public interface KycDocumentDomainMapper {

    @Mapping(target = "kyc", ignore = true)
    @Mapping(target = "type", expression = "java(domain.getType().name())")
    @Mapping(target = "status", expression = "java(domain.getStatus().name())")
    @Mapping(target = "uploadedAt", ignore = true)
    KycDocumentEntity toEntity(KycDocument domain);

    @Mapping(target = "kycId", source = "kyc.kycId")
    @Mapping(target = "type", expression = "java(KycDocumentType.valueOf(entity.getType()))")
    @Mapping(target = "status", expression = "java(KycDocumentStatus.valueOf(entity.getStatus()))")
    KycDocument toDomain(KycDocumentEntity entity);
}
