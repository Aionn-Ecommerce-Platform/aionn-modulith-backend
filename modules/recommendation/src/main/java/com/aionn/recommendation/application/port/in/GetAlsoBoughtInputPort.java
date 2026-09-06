package com.aionn.recommendation.application.port.in;

import com.aionn.recommendation.application.dto.query.GetAlsoBoughtQuery;
import com.aionn.recommendation.application.dto.result.RecommendationItemResult;

import java.util.List;

public interface GetAlsoBoughtInputPort {

    List<RecommendationItemResult> execute(GetAlsoBoughtQuery query);
}
