package com.aionn.recommendation.application.dto.query;

import com.aionn.sharedkernel.application.query.Query;

public record GetAlsoBoughtQuery(String productId, int limit) implements Query {
}
