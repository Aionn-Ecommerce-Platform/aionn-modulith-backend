package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerId;
import com.aionn.catalog.application.service.StockAlertService;
import com.aionn.platform.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/products/{productId}/variants/{skuId}/stock-alert")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Catalog - Stock alerts", description = "Back-in-stock notifications")
public class StockAlertController {

    private final StockAlertService stockAlertService;

    @PostMapping
    @Operation(summary = "Subscribe to a back-in-stock alert")
    public ResponseEntity<ApiResponse<Void>> subscribe(
            @CurrentOwnerId String userId,
            @PathVariable String productId,
            @PathVariable String skuId) {
        stockAlertService.subscribe(userId, productId, skuId);
        return ResponseEntity.ok(ApiResponse.success("Stock alert registered"));
    }

    @DeleteMapping
    @Operation(summary = "Cancel a back-in-stock alert")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(
            @CurrentOwnerId String userId,
            @PathVariable String productId,
            @PathVariable String skuId) {
        stockAlertService.unsubscribe(userId, skuId);
        return ResponseEntity.ok(ApiResponse.success("Stock alert cancelled"));
    }

    @GetMapping("/status")
    @Operation(summary = "Check a back-in-stock alert")
    public ResponseEntity<ApiResponse<StockAlertStatusResponse>> status(
            @CurrentOwnerId String userId,
            @PathVariable String productId,
            @PathVariable String skuId) {
        return ResponseEntity.ok(ApiResponse.success(
                new StockAlertStatusResponse(stockAlertService.isSubscribed(userId, skuId)),
                "Stock alert status fetched"));
    }

    public record StockAlertStatusResponse(boolean subscribed) {
    }
}
