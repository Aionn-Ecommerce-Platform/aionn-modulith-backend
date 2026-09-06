package com.aionn.recommendation.application.dto.query;

import com.aionn.sharedkernel.application.query.Query;

import java.util.List;

public record GetCartSuggestionsQuery(String userId, List<String> cartSkuIds, int limit)
        implements Query {

    public GetCartSuggestionsQuery {
        cartSkuIds = cartSkuIds == null ? List.of() : List.copyOf(cartSkuIds);
    }
}
