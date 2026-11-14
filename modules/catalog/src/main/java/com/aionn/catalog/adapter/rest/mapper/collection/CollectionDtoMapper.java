package com.aionn.catalog.adapter.rest.mapper.collection;

import com.aionn.catalog.adapter.rest.dto.collection.response.CollectionResponse;
import com.aionn.catalog.application.port.out.merchant.CollectionPersistencePort.CollectionView;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CollectionDtoMapper {

    CollectionResponse toResponse(CollectionView view);

    List<CollectionResponse> toResponses(List<CollectionView> views);
}
