package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetReportedReviewsQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;

public interface GetReportedReviewsInputPort {
    PageResult<ReviewResult> execute(GetReportedReviewsQuery query);
}
