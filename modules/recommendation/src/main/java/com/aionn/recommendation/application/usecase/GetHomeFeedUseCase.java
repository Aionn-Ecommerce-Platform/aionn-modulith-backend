package com.aionn.recommendation.application.usecase;

import com.aionn.recommendation.application.dto.query.GetHomeFeedQuery;
import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.in.GetHomeFeedInputPort;
import com.aionn.recommendation.application.service.RecommendationReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetHomeFeedUseCase implements GetHomeFeedInputPort {

    private final RecommendationReadService recommendationReadService;

    @Override
    public List<RecommendationItemResult> execute(GetHomeFeedQuery query) {
        return recommendationReadService.homeFeed(query.userId(), query.limit());
    }
}
