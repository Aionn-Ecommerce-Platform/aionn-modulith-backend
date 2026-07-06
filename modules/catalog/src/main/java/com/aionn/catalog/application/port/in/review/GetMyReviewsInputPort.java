package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetMyReviewsQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;

public interface GetMyReviewsInputPort {
    PageResult<ReviewResult> execute(GetMyReviewsQuery query);
}
