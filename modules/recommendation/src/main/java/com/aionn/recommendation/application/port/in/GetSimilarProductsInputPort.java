package com.aionn.recommendation.application.port.in;

import com.aionn.recommendation.application.dto.query.GetSimilarProductsQuery;
import com.aionn.recommendation.application.dto.result.RecommendationItemResult;

import java.util.List;

public interface GetSimilarProductsInputPort {

    List<RecommendationItemResult> execute(GetSimilarProductsQuery query);
}
