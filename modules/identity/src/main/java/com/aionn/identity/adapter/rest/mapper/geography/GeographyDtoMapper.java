package com.aionn.identity.adapter.rest.mapper.geography;

import com.aionn.identity.adapter.rest.dto.geography.response.GeographyResponse;
import com.aionn.identity.application.dto.geography.result.GeographyResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GeographyDtoMapper {

    GeographyResponse toResponse(GeographyResult result);

    List<GeographyResponse> toResponses(List<GeographyResult> results);
}
