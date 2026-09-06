package com.aionn.recommendation.application.port.in;

import com.aionn.recommendation.application.dto.query.GetCartSuggestionsQuery;
import com.aionn.recommendation.application.dto.result.RecommendationItemResult;

import java.util.List;

public interface GetCartSuggestionsInputPort {

    List<RecommendationItemResult> execute(GetCartSuggestionsQuery query);
}
