package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.feedback.request.SubmitFeedbackRequest;
import com.aionn.identity.adapter.rest.dto.feedback.response.FeedbackResponse;
import com.aionn.identity.adapter.rest.mapper.feedback.FeedbackDtoMapper;
import com.aionn.identity.application.port.in.feedback.ListMyFeedbackQueryPort;
import com.aionn.identity.application.port.in.feedback.SubmitFeedbackInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
@Tag(name = "Identity - Feedback", description = "User feedback / contact-us endpoints")
public class FeedbackController {

    private final SubmitFeedbackInputPort submitFeedbackInputPort;
    private final ListMyFeedbackQueryPort listMyFeedbackQueryPort;
    private final FeedbackDtoMapper feedbackDtoMapper;

    @PostMapping
    @Operation(summary = "Submit feedback", description = "Public endpoint, accepts both authenticated and anonymous submissions")
    public ResponseEntity<ApiResponse<FeedbackResponse>> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitFeedbackRequest request) {
        // Spring Security populates an AnonymousAuthenticationToken for
        // unauthenticated requests whose isAuthenticated() returns true and
        // whose name is "anonymousUser" — treat that as no user, otherwise
        // "anonymousUser" would end up in the SubmitFeedbackCommand.
        String userId = (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken))
                        ? authentication.getName()
                        : null;
        FeedbackResponse response = feedbackDtoMapper.toResponse(
                submitFeedbackInputPort.execute(feedbackDtoMapper.toSubmitCommand(userId, request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Feedback submitted"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my feedbacks")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> listMine(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                feedbackDtoMapper.toResponses(listMyFeedbackQueryPort.execute(authentication.getName())),
                "Feedbacks fetched"));
    }
}
