package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.question.request.AnswerQuestionRequest;
import com.aionn.catalog.adapter.rest.dto.question.request.AskQuestionRequest;
import com.aionn.catalog.adapter.rest.dto.question.response.ProductQuestionResponse;
import com.aionn.catalog.adapter.rest.mapper.question.ProductQuestionDtoMapper;
import com.aionn.catalog.adapter.rest.support.session.CurrentMerchantId;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerId;
import com.aionn.catalog.application.dto.question.result.ProductQuestionResult;
import com.aionn.catalog.application.service.ProductQuestionService;
import com.aionn.platform.web.response.ApiResponse;
import com.aionn.platform.web.response.PageMetadata;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalog - Product Q&A", description = "Buyer questions and merchant answers")
public class ProductQuestionController {

    private final ProductQuestionService questionService;
    private final ProductQuestionDtoMapper questionDtoMapper;

    @GetMapping("/products/{productId}/questions")
    @Operation(summary = "List product questions")
    public ResponseEntity<ApiResponse<List<ProductQuestionResponse>>> listByProduct(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        ProductQuestionService.QuestionPage result = questionService.listByProduct(productId, page, size);
        return ResponseEntity.ok(ApiResponse.successWithPaging(
                questionDtoMapper.toResponses(result.content()),
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
        ProductQuestionResult question = questionService.ask(userId, productId, request.question());
        return ResponseEntity.ok(ApiResponse.success(
                questionDtoMapper.toResponse(question), "Question submitted"));
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
        ProductQuestionResult question = questionService.answer(merchantId, questionId, request.answer());
        return ResponseEntity.ok(ApiResponse.success(
                questionDtoMapper.toResponse(question), "Answer submitted"));
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
                questionDtoMapper.toResponses(result.content()),
                PageMetadata.of(page, size, result.totalElements()),
                "Questions fetched"));
    }
}
