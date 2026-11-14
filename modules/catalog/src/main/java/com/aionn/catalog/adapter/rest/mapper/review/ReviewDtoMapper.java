package com.aionn.catalog.adapter.rest.mapper.review;

import com.aionn.catalog.adapter.rest.dto.review.request.MerchantReplyRequest;
import com.aionn.catalog.adapter.rest.dto.review.request.ReportReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.request.SubmitReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.request.UpdateReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.response.ReviewResponse;
import com.aionn.catalog.adapter.rest.dto.review.response.ReviewEligibilityResponse;
import com.aionn.catalog.application.dto.review.command.AdminDeleteReviewCommand;
import com.aionn.catalog.application.dto.review.command.DeleteReviewCommand;
import com.aionn.catalog.application.dto.review.command.HideReviewCommand;
import com.aionn.catalog.application.dto.review.command.MerchantReplyCommand;
import com.aionn.catalog.application.dto.review.command.ReportReviewCommand;
import com.aionn.catalog.application.dto.review.command.RestoreReviewCommand;
import com.aionn.catalog.application.dto.review.command.SubmitReviewCommand;
import com.aionn.catalog.application.dto.review.command.UpdateReviewCommand;
import com.aionn.catalog.application.dto.review.query.CheckReviewEligibilityQuery;
import com.aionn.catalog.application.dto.review.query.GetAdminProductReviewsQuery;
import com.aionn.catalog.application.dto.review.query.GetMerchantReviewsQuery;
import com.aionn.catalog.application.dto.review.query.GetMyReviewsQuery;
import com.aionn.catalog.application.dto.review.query.GetProductRatingSummaryQuery;
import com.aionn.catalog.application.dto.review.query.GetReportedReviewsQuery;
import com.aionn.catalog.application.dto.review.query.GetReviewsByProductQuery;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.dto.review.result.ReviewEligibilityResult;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewDtoMapper {

    SubmitReviewCommand toSubmitReviewCommand(String userId, String productId, SubmitReviewRequest request);

    UpdateReviewCommand toUpdateReviewCommand(String userId, String reviewId, UpdateReviewRequest request);

    MerchantReplyCommand toMerchantReplyCommand(String ownerId, String reviewId, MerchantReplyRequest request);

    ReportReviewCommand toReportReviewCommand(String ownerId, String reviewId, ReportReviewRequest request);

    default DeleteReviewCommand toDeleteCommand(String userId, String reviewId) {
        return new DeleteReviewCommand(userId, reviewId);
    }

    default HideReviewCommand toHideCommand(String adminId, String reviewId) {
        return new HideReviewCommand(adminId, reviewId);
    }

    default AdminDeleteReviewCommand toAdminDeleteCommand(String adminId, String reviewId) {
        return new AdminDeleteReviewCommand(adminId, reviewId);
    }

    default RestoreReviewCommand toRestoreCommand(String adminId, String reviewId) {
        return new RestoreReviewCommand(adminId, reviewId);
    }

    ReviewResponse toResponse(ReviewResult result);

    List<ReviewResponse> toResponses(List<ReviewResult> results);

    ReviewEligibilityResponse toResponse(ReviewEligibilityResult result);

    default GetReviewsByProductQuery toReviewsByProductQuery(String productId, int page, int size) {
        return new GetReviewsByProductQuery(productId, OffsetPagination.of(page, size));
    }

    default GetProductRatingSummaryQuery toRatingSummaryQuery(String productId) {
        return new GetProductRatingSummaryQuery(productId);
    }

    default GetMyReviewsQuery toMyReviewsQuery(String userId, int page, int size) {
        return new GetMyReviewsQuery(userId, OffsetPagination.of(page, size));
    }

    default GetMerchantReviewsQuery toMerchantReviewsQuery(String ownerId, Boolean replied, int page, int size) {
        return new GetMerchantReviewsQuery(ownerId, replied, OffsetPagination.of(page, size));
    }

    default GetReportedReviewsQuery toReportedReviewsQuery(int page, int size) {
        return new GetReportedReviewsQuery(OffsetPagination.of(page, size));
    }

    default GetAdminProductReviewsQuery toAdminProductReviewsQuery(String productId, int page, int size) {
        return new GetAdminProductReviewsQuery(productId, OffsetPagination.of(page, size));
    }

    default CheckReviewEligibilityQuery toEligibilityQuery(String userId, String productId) {
        return new CheckReviewEligibilityQuery(userId, productId);
    }
}
