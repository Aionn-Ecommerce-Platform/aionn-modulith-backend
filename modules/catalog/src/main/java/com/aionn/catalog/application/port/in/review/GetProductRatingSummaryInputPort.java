package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.review.query.GetProductRatingSummaryQuery;
import com.aionn.catalog.application.dto.review.result.RatingSummary;

public interface GetProductRatingSummaryInputPort {
    RatingSummary execute(GetProductRatingSummaryQuery query);
}
