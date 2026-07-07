package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.ProductReview;
import com.aionn.catalog.infrastructure.persistence.entity.ProductReviewEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReviewDomainMapper {

    @Mapping(target = "status", expression = "java(domain.getStatus() != null ? domain.getStatus().name() : null)")
    ProductReviewEntity toEntity(ProductReview domain);

    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? com.aionn.catalog.domain.valueobject.ReviewStatus.valueOf(entity.getStatus()) : com.aionn.catalog.domain.valueobject.ReviewStatus.VISIBLE)")
    @Mapping(target = "imageUrls", source = "imageUrls")
    ProductReview toDomain(ProductReviewEntity entity);
}
