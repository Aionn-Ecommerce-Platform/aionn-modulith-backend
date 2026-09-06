package com.aionn.recommendation.application.port.in;

import com.aionn.recommendation.application.dto.query.GetHomeFeedQuery;
import com.aionn.recommendation.application.dto.result.RecommendationItemResult;

import java.util.List;

public interface GetHomeFeedInputPort {

    List<RecommendationItemResult> execute(GetHomeFeedQuery query);
}
