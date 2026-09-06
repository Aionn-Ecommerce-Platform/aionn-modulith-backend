package com.aionn.recommendation.application.usecase;

import com.aionn.recommendation.application.dto.query.GetCartSuggestionsQuery;
import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.in.GetCartSuggestionsInputPort;
import com.aionn.recommendation.application.service.RecommendationReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCartSuggestionsUseCase implements GetCartSuggestionsInputPort {

    private final RecommendationReadService recommendationReadService;

    @Override
    public List<RecommendationItemResult> execute(GetCartSuggestionsQuery query) {
        return recommendationReadService.cartSuggestions(
                query.userId(), query.cartSkuIds(), query.limit());
    }
}
