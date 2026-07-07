package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.infrastructure.persistence.entity.MerchantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MerchantDomainMapper {

    @Mapping(target = "status", expression = "java(merchant.getStatus() != null ? merchant.getStatus().name() : null)")
    MerchantEntity toEntity(Merchant merchant);

    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? com.aionn.catalog.domain.valueobject.MerchantStatus.valueOf(entity.getStatus()) : null)")
    Merchant toDomain(MerchantEntity entity);
}
