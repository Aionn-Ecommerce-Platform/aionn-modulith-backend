package com.aionn.recommendation.application.usecase;

import com.aionn.recommendation.application.dto.query.GetSimilarProductsQuery;
import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.in.GetSimilarProductsInputPort;
import com.aionn.recommendation.application.service.RecommendationReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSimilarProductsUseCase implements GetSimilarProductsInputPort {

    private final RecommendationReadService recommendationReadService;

    @Override
    public List<RecommendationItemResult> execute(GetSimilarProductsQuery query) {
        return recommendationReadService.similarProducts(query.productId(), query.limit());
    }
}
