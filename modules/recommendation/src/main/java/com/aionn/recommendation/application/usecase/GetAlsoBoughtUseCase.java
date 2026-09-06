package com.aionn.recommendation.application.usecase;

import com.aionn.recommendation.application.dto.query.GetAlsoBoughtQuery;
import com.aionn.recommendation.application.dto.result.RecommendationItemResult;
import com.aionn.recommendation.application.port.in.GetAlsoBoughtInputPort;
import com.aionn.recommendation.application.service.RecommendationReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAlsoBoughtUseCase implements GetAlsoBoughtInputPort {

    private final RecommendationReadService recommendationReadService;

    @Override
    public List<RecommendationItemResult> execute(GetAlsoBoughtQuery query) {
        return recommendationReadService.alsoBought(query.productId(), query.limit());
    }
}
