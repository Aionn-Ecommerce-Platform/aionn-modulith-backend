package com.aionn.recommendation.application.dto.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetHomeFeedQuery(String userId, int limit) implements Query {
}
