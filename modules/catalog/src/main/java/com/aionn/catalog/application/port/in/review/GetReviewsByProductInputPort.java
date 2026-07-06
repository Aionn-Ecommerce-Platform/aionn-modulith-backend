package com.aionn.catalog.application.port.in.review;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.query.GetReviewsByProductQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;

public interface GetReviewsByProductInputPort {
    PageResult<ReviewResult> execute(GetReviewsByProductQuery query);
}
