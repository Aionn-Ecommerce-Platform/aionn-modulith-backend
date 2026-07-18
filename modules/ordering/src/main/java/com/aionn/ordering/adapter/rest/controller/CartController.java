package com.aionn.ordering.adapter.rest.controller;

import com.aionn.ordering.adapter.rest.dto.request.AddCartItemRequest;
import com.aionn.ordering.adapter.rest.dto.request.ApplyVoucherRequest;
import com.aionn.ordering.adapter.rest.dto.request.UpdateCartItemRequest;
import com.aionn.ordering.adapter.rest.dto.response.CartResponse;
import com.aionn.ordering.adapter.rest.mapper.OrderingDtoMapper;
import com.aionn.ordering.adapter.rest.support.session.CurrentUserId;
import com.aionn.ordering.application.dto.cart.command.AddItemCommand;
import com.aionn.ordering.application.dto.cart.command.ApplyVoucherCommand;
import com.aionn.ordering.application.dto.cart.command.ClearCartCommand;
import com.aionn.ordering.application.dto.cart.command.RemoveItemCommand;
import com.aionn.ordering.application.dto.cart.command.RemoveVoucherCommand;
import com.aionn.ordering.application.dto.cart.command.UpdateItemQtyCommand;
import com.aionn.ordering.application.port.in.cart.*;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ordering/cart")
@RequiredArgsConstructor
@Tag(name = "Ordering - Cart", description = "User shopping cart endpoints")
public class CartController {

    private final AddItemInputPort addItemInputPort;
    private final UpdateItemQtyInputPort updateItemQtyInputPort;
    private final RemoveItemInputPort removeItemInputPort;
    private final ClearCartInputPort clearCartInputPort;
    private final ApplyVoucherInputPort applyVoucherInputPort;
    private final RemoveVoucherInputPort removeVoucherInputPort;
    private final GetMyCartInputPort getMyCartInputPort;
    private final OrderingDtoMapper dtoMapper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my cart")
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(@CurrentUserId String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                dtoMapper.toResponse(getMyCartInputPort.execute(userId)),
                "Cart fetched"));
    }

    @PostMapping("/items")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add item")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @CurrentUserId String userId,
            @Valid @RequestBody AddCartItemRequest request) {
        CartResponse response = dtoMapper.toResponse(addItemInputPort.execute(new AddItemCommand(
                userId, request.skuId(), request.qty())));
        return ResponseEntity.ok(ApiResponse.success(response, "Item added"));
    }

    @PutMapping("/items/{skuId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update item qty")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @CurrentUserId String userId,
            @PathVariable String skuId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse response = dtoMapper.toResponse(updateItemQtyInputPort.execute(new UpdateItemQtyCommand(
                userId, skuId, request.newQty())));
        return ResponseEntity.ok(ApiResponse.success(response, "Item updated"));
    }

    @DeleteMapping("/items/{skuId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove item")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @CurrentUserId String userId,
            @PathVariable String skuId) {
        CartResponse response = dtoMapper.toResponse(removeItemInputPort.execute(new RemoveItemCommand(userId, skuId)));
        return ResponseEntity.ok(ApiResponse.success(response, "Item removed"));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Clear cart")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(@CurrentUserId String userId) {
        CartResponse response = dtoMapper.toResponse(clearCartInputPort.execute(new ClearCartCommand(userId, "user-cleared")));
        return ResponseEntity.ok(ApiResponse.success(response, "Cart cleared"));
    }

    @PostMapping("/voucher")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Apply voucher")
    public ResponseEntity<ApiResponse<CartResponse>> applyVoucher(
            @CurrentUserId String userId,
            @Valid @RequestBody ApplyVoucherRequest request) {
        CartResponse response = dtoMapper.toResponse(applyVoucherInputPort.execute(new ApplyVoucherCommand(
                userId, request.voucherCode())));
        return ResponseEntity.ok(ApiResponse.success(response, "Voucher applied"));
    }

    @DeleteMapping("/voucher")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove voucher")
    public ResponseEntity<ApiResponse<CartResponse>> removeVoucher(@CurrentUserId String userId) {
        CartResponse response = dtoMapper.toResponse(removeVoucherInputPort.execute(new RemoveVoucherCommand(userId)));
        return ResponseEntity.ok(ApiResponse.success(response, "Voucher removed"));
    }
}
