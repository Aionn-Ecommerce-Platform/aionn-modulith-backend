package com.aionn.recommendation.adapter.rest.mapper;

import com.aionn.recommendation.adapter.rest.dto.recommendation.response.RecommendationResponse;
import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Result to response conversion. {@code skuIds} is intentionally not mapped: it exists so availability
 * filtering can run after the cache, and clients recommend products rather than SKUs.
 */
@Mapper(componentModel = "spring")
public interface RecommendationDtoMapper {

    RecommendationResponse toResponse(RecommendationItemResult result);

    List<RecommendationResponse> toResponses(List<RecommendationItemResult> results);
}
