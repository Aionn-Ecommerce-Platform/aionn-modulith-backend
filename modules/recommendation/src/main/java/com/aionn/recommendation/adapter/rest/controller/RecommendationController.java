package com.aionn.recommendation.adapter.rest.controller;

import com.aionn.recommendation.adapter.rest.dto.recommendation.response.RecommendationResponse;
import com.aionn.recommendation.adapter.rest.mapper.RecommendationDtoMapper;
import com.aionn.recommendation.application.dto.query.GetAlsoBoughtQuery;
import com.aionn.recommendation.application.dto.query.GetCartSuggestionsQuery;
import com.aionn.recommendation.application.dto.query.GetHomeFeedQuery;
import com.aionn.recommendation.application.dto.query.GetSimilarProductsQuery;
import com.aionn.recommendation.application.port.in.GetAlsoBoughtInputPort;
import com.aionn.recommendation.application.port.in.GetCartSuggestionsInputPort;
import com.aionn.recommendation.application.port.in.GetHomeFeedInputPort;
import com.aionn.recommendation.application.port.in.GetSimilarProductsInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * There is no endpoint for recording interactions. Behavioural signals arrive as integration events
 * from the module that owns the action, so a client cannot write another user's history.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Personalised and related product recommendations")
public class RecommendationController {

    private static final String MSG_FETCHED = "Recommendations fetched";

    private final GetHomeFeedInputPort getHomeFeedInputPort;
    private final GetSimilarProductsInputPort getSimilarProductsInputPort;
    private final GetAlsoBoughtInputPort getAlsoBoughtInputPort;
    private final GetCartSuggestionsInputPort getCartSuggestionsInputPort;
    private final RecommendationDtoMapper recommendationDtoMapper;

    @GetMapping("/home")
    @Operation(summary = "Get the personalised home feed")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getHomeFeed(
            Authentication authentication,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        // Optional authentication: anonymous callers get trending instead of a personalised feed.
        String userId = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ApiResponse.success(
                recommendationDtoMapper.toResponses(
                        getHomeFeedInputPort.execute(new GetHomeFeedQuery(userId, limit))),
                MSG_FETCHED));
    }

    @GetMapping("/products/{productId}/similar")
    @Operation(summary = "Get products similar to the given product")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getSimilarProducts(
            @PathVariable String productId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationDtoMapper.toResponses(getSimilarProductsInputPort
                        .execute(new GetSimilarProductsQuery(productId, limit))),
                MSG_FETCHED));
    }

    @GetMapping("/products/{productId}/also-bought")
    @Operation(summary = "Get products frequently bought with the given product")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getAlsoBought(
            @PathVariable String productId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationDtoMapper.toResponses(
                        getAlsoBoughtInputPort.execute(new GetAlsoBoughtQuery(productId, limit))),
                MSG_FETCHED));
    }

    @GetMapping("/cart/suggestions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get suggestions that complement the current cart")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getCartSuggestions(
            Authentication authentication,
            @RequestParam @NotEmpty List<String> skuIds,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationDtoMapper.toResponses(getCartSuggestionsInputPort
                        .execute(new GetCartSuggestionsQuery(authentication.getName(), skuIds, limit))),
                MSG_FETCHED));
    }
}
