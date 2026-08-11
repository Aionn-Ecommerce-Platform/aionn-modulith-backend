package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetAdminProductReviewsQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;

public interface GetAdminProductReviewsInputPort {
    PageResult<ReviewResult> execute(GetAdminProductReviewsQuery query);
}
