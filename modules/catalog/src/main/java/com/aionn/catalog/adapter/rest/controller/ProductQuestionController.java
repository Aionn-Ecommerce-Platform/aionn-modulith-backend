package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.support.session.CurrentMerchantId;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerId;
import com.aionn.catalog.application.port.out.merchant.ProductQuestionPersistencePort;
import com.aionn.catalog.application.service.ProductQuestionService;
import com.aionn.contracts.port.identity.UserProfileLookupPort;
import com.aionn.platform.web.response.ApiResponse;
import com.aionn.platform.web.response.PageMetadata;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalog - Product Q&A", description = "Buyer questions and merchant answers")
public class ProductQuestionController {

    private final ProductQuestionService questionService;
    private final UserProfileLookupPort userProfileLookupPort;

    @GetMapping("/products/{productId}/questions")
    @Operation(summary = "List product questions")
    public ResponseEntity<ApiResponse<List<ProductQuestionResponse>>> listByProduct(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        ProductQuestionService.QuestionPage result = questionService.listByProduct(productId, page, size);
        return ResponseEntity.ok(ApiResponse.successWithPaging(
                toResponses(result.content()),
                PageMetadata.of(page, size, result.totalElements()),
                "Questions fetched"));
    }

    @PostMapping("/products/{productId}/questions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ask a question about a product")
    public ResponseEntity<ApiResponse<ProductQuestionResponse>> ask(
            @CurrentOwnerId String userId,
            @PathVariable String productId,
            @Valid @RequestBody AskQuestionRequest request) {
        var question = questionService.ask(userId, productId, request.question());
        return ResponseEntity.ok(ApiResponse.success(
                toResponses(List.of(question)).get(0), "Question submitted"));
    }

    @DeleteMapping("/questions/{questionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete my question")
    public ResponseEntity<ApiResponse<Void>> delete(
            @CurrentOwnerId String userId,
            @PathVariable String questionId) {
        questionService.delete(userId, questionId);
        return ResponseEntity.ok(ApiResponse.success("Question deleted"));
    }

    @PostMapping("/questions/{questionId}/answer")
    @PreAuthorize("hasAnyAuthority('ROLE_MERCHANT','ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Merchant answers a question")
    public ResponseEntity<ApiResponse<ProductQuestionResponse>> answer(
            @CurrentMerchantId String merchantId,
            @PathVariable String questionId,
            @Valid @RequestBody AnswerQuestionRequest request) {
        var question = questionService.answer(merchantId, questionId, request.answer());
        return ResponseEntity.ok(ApiResponse.success(
                toResponses(List.of(question)).get(0), "Answer submitted"));
    }

    @GetMapping("/merchant/questions")
    @PreAuthorize("hasAnyAuthority('ROLE_MERCHANT','ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "List questions for the authenticated merchant")
    public ResponseEntity<ApiResponse<List<ProductQuestionResponse>>> listByMerchant(
            @CurrentMerchantId String merchantId,
            @RequestParam(required = false) Boolean answered,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        ProductQuestionService.QuestionPage result =
                questionService.listByMerchant(merchantId, answered, page, size);
        return ResponseEntity.ok(ApiResponse.successWithPaging(
                toResponses(result.content()),
                PageMetadata.of(page, size, result.totalElements()),
                "Questions fetched"));
    }

    private List<ProductQuestionResponse> toResponses(List<ProductQuestionPersistencePort.Question> questions) {
        Set<String> userIds = questions.stream()
                .map(ProductQuestionPersistencePort.Question::userId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, UserProfileLookupPort.UserProfile> profiles = userIds.isEmpty()
                ? Map.of()
                : userProfileLookupPort.findProfiles(userIds);
        return questions.stream()
                .map(question -> {
                    var profile = profiles.get(question.userId());
                    return new ProductQuestionResponse(
                            question.questionId(),
                            question.productId(),
                            question.userId(),
                            question.question(),
                            question.answer(),
                            question.answeredBy(),
                            question.answeredAt(),
                            question.createdAt(),
                            profile == null ? null : profile.username(),
                            profile == null ? null : profile.displayName(),
                            profile == null ? null : profile.avatarUrl());
                })
                .toList();
    }

    public record AskQuestionRequest(
            @NotBlank @Size(min = 5, max = 2000) String question) {
    }

    public record AnswerQuestionRequest(
            @NotBlank @Size(min = 1, max = 2000) String answer) {
    }

    public record ProductQuestionResponse(
            String questionId,
            String productId,
            String userId,
            String question,
            String answer,
            String answeredBy,
            Instant answeredAt,
            Instant createdAt,
            String userName,
            String userDisplayName,
            String userAvatarUrl) {
    }
}
