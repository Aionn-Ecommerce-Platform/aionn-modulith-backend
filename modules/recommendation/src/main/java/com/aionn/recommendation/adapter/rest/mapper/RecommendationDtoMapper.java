package com.aionn.recommendation.adapter.rest.mapper;

import com.aionn.recommendation.adapter.rest.dto.recommendation.response.RecommendationResponse;
import com.aionn.recommendation.application.dto.query.GetAlsoBoughtQuery;
import com.aionn.recommendation.application.dto.query.GetCartSuggestionsQuery;
import com.aionn.recommendation.application.dto.query.GetHomeFeedQuery;
import com.aionn.recommendation.application.dto.query.GetSimilarProductsQuery;
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

    default GetHomeFeedQuery toHomeFeedQuery(String userId, int limit) {
        return new GetHomeFeedQuery(userId, limit);
    }

    default GetSimilarProductsQuery toSimilarProductsQuery(String productId, int limit) {
        return new GetSimilarProductsQuery(productId, limit);
    }

    default GetAlsoBoughtQuery toAlsoBoughtQuery(String productId, int limit) {
        return new GetAlsoBoughtQuery(productId, limit);
    }

    default GetCartSuggestionsQuery toCartSuggestionsQuery(String userId, List<String> skuIds, int limit) {
        return new GetCartSuggestionsQuery(userId, skuIds, limit);
    }
}
