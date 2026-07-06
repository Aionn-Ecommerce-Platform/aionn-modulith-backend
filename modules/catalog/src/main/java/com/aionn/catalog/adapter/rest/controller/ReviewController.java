package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.review.MerchantReplyRequest;
import com.aionn.catalog.adapter.rest.dto.review.ReportReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.SubmitReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.UpdateReviewRequest;
import com.aionn.catalog.adapter.rest.mapper.review.ReviewDtoMapper;
import com.aionn.catalog.adapter.rest.support.session.CurrentAdminId;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerId;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.command.AdminDeleteReviewCommand;
import com.aionn.catalog.application.dto.review.command.DeleteReviewCommand;
import com.aionn.catalog.application.dto.review.command.HideReviewCommand;
import com.aionn.catalog.application.dto.review.command.RestoreReviewCommand;
import com.aionn.catalog.application.dto.review.query.CheckReviewEligibilityQuery;
import com.aionn.catalog.application.dto.review.query.GetMyReviewsQuery;
import com.aionn.catalog.application.dto.review.query.GetProductRatingSummaryQuery;
import com.aionn.catalog.application.dto.review.query.GetReportedReviewsQuery;
import com.aionn.catalog.application.dto.review.query.GetReviewsByProductQuery;
import com.aionn.catalog.application.dto.review.result.RatingSummary;
import com.aionn.catalog.application.dto.review.result.ReviewEligibilityResult;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.port.in.review.AdminDeleteReviewInputPort;
import com.aionn.catalog.application.port.in.review.CheckReviewEligibilityInputPort;
import com.aionn.catalog.application.port.in.review.DeleteReviewInputPort;
import com.aionn.catalog.application.port.in.review.GetMyReviewsInputPort;
import com.aionn.catalog.application.port.in.review.GetProductRatingSummaryInputPort;
import com.aionn.catalog.application.port.in.review.GetReportedReviewsInputPort;
import com.aionn.catalog.application.port.in.review.GetReviewsByProductInputPort;
import com.aionn.catalog.application.port.in.review.HideReviewInputPort;
import com.aionn.catalog.application.port.in.review.MerchantReplyReviewInputPort;
import com.aionn.catalog.application.port.in.review.ReportReviewInputPort;
import com.aionn.catalog.application.port.in.review.RestoreReviewInputPort;
import com.aionn.catalog.application.port.in.review.SubmitReviewInputPort;
import com.aionn.catalog.application.port.in.review.UpdateReviewInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.response.PageMetadata;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalog - Review", description = "Product review submit, update, reply, report, moderation")
public class ReviewController {

    private final SubmitReviewInputPort submitReviewInputPort;
    private final UpdateReviewInputPort updateReviewInputPort;
    private final DeleteReviewInputPort deleteReviewInputPort;
    private final MerchantReplyReviewInputPort merchantReplyReviewInputPort;
    private final ReportReviewInputPort reportReviewInputPort;
    private final HideReviewInputPort hideReviewInputPort;
    private final AdminDeleteReviewInputPort adminDeleteReviewInputPort;
    private final RestoreReviewInputPort restoreReviewInputPort;
    private final GetReviewsByProductInputPort getReviewsByProductInputPort;
    private final GetMyReviewsInputPort getMyReviewsInputPort;
    private final GetReportedReviewsInputPort getReportedReviewsInputPort;
    private final GetProductRatingSummaryInputPort getProductRatingSummaryInputPort;
    private final CheckReviewEligibilityInputPort checkReviewEligibilityInputPort;
    private final ReviewDtoMapper reviewDtoMapper;

    @PostMapping("/products/{productId}/reviews")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit review")
    public ResponseEntity<ApiResponse<ReviewResult>> submit(
            @CurrentOwnerId String userId,
            @PathVariable String productId,
            @Valid @RequestBody SubmitReviewRequest request) {
        return ApiResponse.createdResponse("Review submitted",
                submitReviewInputPort.execute(
                        reviewDtoMapper.toSubmitReviewCommand(userId, productId, request)));
    }

    @PutMapping("/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update own review")
    public ResponseEntity<ApiResponse<ReviewResult>> update(
            @CurrentOwnerId String userId,
            @PathVariable String reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                updateReviewInputPort.execute(
                        reviewDtoMapper.toUpdateReviewCommand(userId, reviewId, request)),
                "Review updated"));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete own review")
    public ResponseEntity<Void> delete(
            @CurrentOwnerId String userId,
            @PathVariable String reviewId) {
        deleteReviewInputPort.execute(new DeleteReviewCommand(userId, reviewId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reviews/{reviewId}/reply")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Merchant replies to review")
    public ResponseEntity<ApiResponse<ReviewResult>> reply(
            @CurrentOwnerId String ownerId,
            @PathVariable String reviewId,
            @Valid @RequestBody MerchantReplyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                merchantReplyReviewInputPort.execute(
                        reviewDtoMapper.toMerchantReplyCommand(ownerId, reviewId, request)),
                "Reply saved"));
    }

    @PostMapping("/reviews/{reviewId}/report")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Merchant reports abusive review")
    public ResponseEntity<ApiResponse<ReviewResult>> report(
            @CurrentOwnerId String ownerId,
            @PathVariable String reviewId,
            @Valid @RequestBody ReportReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                reportReviewInputPort.execute(
                        reviewDtoMapper.toReportReviewCommand(ownerId, reviewId, request)),
                "Review reported"));
    }

    @PostMapping("/admin/reviews/{reviewId}/hide")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Admin hides a review")
    public ResponseEntity<ApiResponse<ReviewResult>> adminHide(
            @CurrentAdminId String adminId,
            @PathVariable String reviewId) {
        return ResponseEntity.ok(ApiResponse.success(
                hideReviewInputPort.execute(new HideReviewCommand(adminId, reviewId)),
                "Review hidden"));
    }

    @DeleteMapping("/admin/reviews/{reviewId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Admin soft-deletes a review")
    public ResponseEntity<Void> adminDelete(
            @CurrentAdminId String adminId,
            @PathVariable String reviewId) {
        adminDeleteReviewInputPort.execute(new AdminDeleteReviewCommand(adminId, reviewId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/reviews/{reviewId}/restore")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Admin restores a reported review")
    public ResponseEntity<ApiResponse<ReviewResult>> adminRestore(
            @CurrentAdminId String adminId,
            @PathVariable String reviewId) {
        return ResponseEntity.ok(ApiResponse.success(
                restoreReviewInputPort.execute(new RestoreReviewCommand(adminId, reviewId)),
                "Review restored"));
    }

    @GetMapping("/products/{productId}/reviews")
    @Operation(summary = "List visible reviews of a product")
    public ResponseEntity<ApiResponse<List<ReviewResult>>> listByProduct(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<ReviewResult> result = getReviewsByProductInputPort.execute(
                new GetReviewsByProductQuery(productId, OffsetPagination.of(page, size)));
        return ResponseEntity.ok(ApiResponse.successWithPaging(
                result.content(),
                PageMetadata.of(result.page(), result.size(), result.totalElements()),
                "Reviews fetched"));
    }

    @GetMapping("/products/{productId}/rating-summary")
    @Operation(summary = "Rating summary for a product")
    public ResponseEntity<ApiResponse<RatingSummary>> ratingSummary(@PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.success(
                getProductRatingSummaryInputPort.execute(new GetProductRatingSummaryQuery(productId)),
                "Rating summary fetched"));
    }

    @GetMapping("/reviews/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my reviews")
    public ResponseEntity<ApiResponse<List<ReviewResult>>> myReviews(
            @CurrentOwnerId String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<ReviewResult> result = getMyReviewsInputPort.execute(
                new GetMyReviewsQuery(userId, OffsetPagination.of(page, size)));
        return ResponseEntity.ok(ApiResponse.successWithPaging(
                result.content(),
                PageMetadata.of(result.page(), result.size(), result.totalElements()),
                "My reviews fetched"));
    }

    @GetMapping("/admin/reviews/reported")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "List reported reviews")
    public ResponseEntity<ApiResponse<List<ReviewResult>>> listReported(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<ReviewResult> result = getReportedReviewsInputPort.execute(
                new GetReportedReviewsQuery(OffsetPagination.of(page, size)));
        return ResponseEntity.ok(ApiResponse.successWithPaging(
                result.content(),
                PageMetadata.of(result.page(), result.size(), result.totalElements()),
                "Reported reviews fetched"));
    }

    @GetMapping("/products/{productId}/reviews/eligibility")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check if authenticated user can review this product")
    public ResponseEntity<ApiResponse<ReviewEligibilityResult>> checkEligibility(
            @CurrentOwnerId String userId,
            @PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.success(
                checkReviewEligibilityInputPort.execute(new CheckReviewEligibilityQuery(userId, productId)),
                "Eligibility checked"));
    }
}
