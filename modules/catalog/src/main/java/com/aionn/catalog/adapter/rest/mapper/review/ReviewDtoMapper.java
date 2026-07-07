package com.aionn.catalog.adapter.rest.mapper.review;

import com.aionn.catalog.adapter.rest.dto.review.MerchantReplyRequest;
import com.aionn.catalog.adapter.rest.dto.review.ReportReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.SubmitReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.UpdateReviewRequest;
import com.aionn.catalog.application.dto.review.command.MerchantReplyCommand;
import com.aionn.catalog.application.dto.review.command.ReportReviewCommand;
import com.aionn.catalog.application.dto.review.command.SubmitReviewCommand;
import com.aionn.catalog.application.dto.review.command.UpdateReviewCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewDtoMapper {

    SubmitReviewCommand toSubmitReviewCommand(String userId, String productId, SubmitReviewRequest request);

    UpdateReviewCommand toUpdateReviewCommand(String userId, String reviewId, UpdateReviewRequest request);

    MerchantReplyCommand toMerchantReplyCommand(String ownerId, String reviewId, MerchantReplyRequest request);

    ReportReviewCommand toReportReviewCommand(String ownerId, String reviewId, ReportReviewRequest request);
}
