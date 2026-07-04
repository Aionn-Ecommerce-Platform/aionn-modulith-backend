package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.feedback.request.AdminChangeFeedbackStatusRequest;
import com.aionn.identity.adapter.rest.dto.feedback.request.AdminReplyFeedbackRequest;
import com.aionn.identity.adapter.rest.dto.feedback.response.FeedbackAnalyticsResponse;
import com.aionn.identity.adapter.rest.dto.feedback.response.FeedbackResponse;
import com.aionn.identity.adapter.rest.mapper.feedback.FeedbackDtoMapper;
import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.application.dto.feedback.result.FeedbackResult;
import com.aionn.identity.application.port.in.feedback.ChangeFeedbackStatusInputPort;
import com.aionn.identity.application.port.in.feedback.GetFeedbackAnalyticsQueryPort;
import com.aionn.identity.application.port.in.feedback.GetAdminFeedbackQueryPort;
import com.aionn.identity.application.port.in.feedback.ListAdminFeedbackQueryPort;
import com.aionn.identity.application.port.in.feedback.ReplyFeedbackInputPort;
import com.aionn.identity.domain.valueobject.FeedbackStatus;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/feedbacks")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
@Tag(name = "Identity - Feedback Admin",
        description = "Admin endpoints to triage, reply and close user feedback")
public class AdminFeedbackController {

    private final ListAdminFeedbackQueryPort listAdminFeedbackQueryPort;
    private final GetAdminFeedbackQueryPort getAdminFeedbackQueryPort;
    private final ReplyFeedbackInputPort replyFeedbackInputPort;
    private final ChangeFeedbackStatusInputPort changeFeedbackStatusInputPort;
    private final GetFeedbackAnalyticsQueryPort getFeedbackAnalyticsQueryPort;
    private final FeedbackDtoMapper feedbackDtoMapper;

    @GetMapping("/analytics")
    @Operation(summary = "Feedback queue analytics (admin)")
    public ResponseEntity<ApiResponse<FeedbackAnalyticsResponse>> analytics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var result = getFeedbackAnalyticsQueryPort.execute(from, to);
        return ResponseEntity.ok(ApiResponse.success(
                feedbackDtoMapper.toAnalyticsResponse(result), "Feedback analytics fetched"));
    }

    @GetMapping
    @Operation(summary = "List feedbacks (admin)",
            description = "List feedbacks paginated, optionally filtered by status")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> list(
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageResult<FeedbackResult> result = listAdminFeedbackQueryPort.execute(status, page, size);
        return ResponseEntity.ok(ApiResponse.successWithPaging(
                feedbackDtoMapper.toResponses(result.content()),
                feedbackDtoMapper.toPageMetadata(result),
                "Feedbacks fetched"));
    }

    @GetMapping("/{feedbackId}")
    @Operation(summary = "Get feedback (admin)")
    public ResponseEntity<ApiResponse<FeedbackResponse>> get(@PathVariable String feedbackId) {
        return ResponseEntity.ok(ApiResponse.success(
                feedbackDtoMapper.toResponse(getAdminFeedbackQueryPort.execute(feedbackId)), "Feedback fetched"));
    }

    @PostMapping("/{feedbackId}/reply")
    @Operation(summary = "Reply to feedback (admin)",
            description = "Attach an admin reply; defaults to moving OPEN -> IN_REVIEW unless newStatus is provided")
    public ResponseEntity<ApiResponse<FeedbackResponse>> reply(
            Authentication authentication,
            @PathVariable String feedbackId,
            @Valid @RequestBody AdminReplyFeedbackRequest request) {
        FeedbackResponse response = feedbackDtoMapper.toResponse(replyFeedbackInputPort.execute(
                feedbackDtoMapper.toReplyCommand(feedbackId, authentication.getName(), request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Feedback reply submitted"));
    }

    @PutMapping("/{feedbackId}/status")
    @Operation(summary = "Change feedback status (admin)")
    public ResponseEntity<ApiResponse<FeedbackResponse>> changeStatus(
            Authentication authentication,
            @PathVariable String feedbackId,
            @Valid @RequestBody AdminChangeFeedbackStatusRequest request) {
        FeedbackResponse response = feedbackDtoMapper.toResponse(changeFeedbackStatusInputPort.execute(
                feedbackDtoMapper.toChangeStatusCommand(feedbackId, authentication.getName(), request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Feedback status updated"));
    }
}
