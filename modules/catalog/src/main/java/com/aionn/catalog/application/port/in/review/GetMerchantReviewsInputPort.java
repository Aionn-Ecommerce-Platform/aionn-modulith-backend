package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetMerchantReviewsQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;

public interface GetMerchantReviewsInputPort {
    PageResult<ReviewResult> execute(GetMerchantReviewsQuery query);
}
