package com.aionn.catalog.adapter.rest.mapper.review;

import com.aionn.catalog.adapter.rest.dto.review.request.MerchantReplyRequest;
import com.aionn.catalog.adapter.rest.dto.review.request.ReportReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.request.SubmitReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.request.UpdateReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.response.ReviewResponse;
import com.aionn.catalog.application.dto.review.command.MerchantReplyCommand;
import com.aionn.catalog.application.dto.review.command.ReportReviewCommand;
import com.aionn.catalog.application.dto.review.command.SubmitReviewCommand;
import com.aionn.catalog.application.dto.review.command.UpdateReviewCommand;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewDtoMapper {

    SubmitReviewCommand toSubmitReviewCommand(String userId, String productId, SubmitReviewRequest request);

    UpdateReviewCommand toUpdateReviewCommand(String userId, String reviewId, UpdateReviewRequest request);

    MerchantReplyCommand toMerchantReplyCommand(String ownerId, String reviewId, MerchantReplyRequest request);

    ReportReviewCommand toReportReviewCommand(String ownerId, String reviewId, ReportReviewRequest request);

    ReviewResponse toResponse(ReviewResult result);

    List<ReviewResponse> toResponses(List<ReviewResult> results);
}
