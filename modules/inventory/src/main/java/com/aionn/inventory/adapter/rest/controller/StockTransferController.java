package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.transfer.CancelTransferRequest;
import com.aionn.inventory.adapter.rest.dto.transfer.CompleteTransferRequest;
import com.aionn.inventory.adapter.rest.dto.transfer.InitiateTransferRequest;
import com.aionn.inventory.application.dto.transfer.command.CancelTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.CompleteTransferCommand;
import com.aionn.inventory.application.dto.transfer.command.InitiateTransferCommand;
import com.aionn.inventory.application.dto.transfer.result.StockTransferResult;
import com.aionn.inventory.application.service.StockTransferService;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory/transfers")
@RequiredArgsConstructor
@Tag(name = "Inventory - Transfer", description = "Stock transfer between warehouses")
public class StockTransferController {

    private final StockTransferService transferService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Initiate transfer")
    public ResponseEntity<ApiResponse<StockTransferResult>> initiate(
            Authentication authentication,
            @Valid @RequestBody InitiateTransferRequest request) {
        StockTransferResult result = transferService.initiate(new InitiateTransferCommand(
                authentication.getName(), request.fromWarehouseId(), request.toWarehouseId(),
                request.skuId(), request.qty()));
        return ApiResponse.createdResponse("Transfer initiated", result);
    }

    @PostMapping("/{transferId}/complete")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Complete transfer")
    public ResponseEntity<ApiResponse<StockTransferResult>> complete(
            Authentication authentication,
            @PathVariable String transferId,
            @Valid @RequestBody CompleteTransferRequest request) {
        StockTransferResult result = transferService.complete(new CompleteTransferCommand(
                authentication.getName(), transferId, request.receivedQty()));
        return ResponseEntity.ok(ApiResponse.success(result, "Transfer completed"));
    }

    @PostMapping("/{transferId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel transfer", description = "Cancel an in-flight transfer; refunds source")
    public ResponseEntity<ApiResponse<StockTransferResult>> cancel(
            Authentication authentication,
            @PathVariable String transferId,
            @Valid @RequestBody CancelTransferRequest request) {
        StockTransferResult result = transferService.cancel(new CancelTransferCommand(
                authentication.getName(), transferId, request.reason()));
        return ResponseEntity.ok(ApiResponse.success(result, "Transfer cancelled"));
    }

    @GetMapping("/{transferId}")
    @Operation(summary = "Get transfer")
    public ResponseEntity<ApiResponse<StockTransferResult>> get(@PathVariable String transferId) {
        return ResponseEntity.ok(ApiResponse.success(transferService.get(transferId), "Transfer fetched"));
    }
}
