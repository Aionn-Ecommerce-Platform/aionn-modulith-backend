package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.product.response.ProductResponse;
import com.aionn.catalog.adapter.rest.mapper.product.ProductDtoMapper;
import com.aionn.catalog.adapter.rest.dto.wishlist.response.WishlistStatusResponse;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerId;
import com.aionn.catalog.application.service.WishlistService;
import com.aionn.platform.web.response.ApiResponse;
import com.aionn.platform.web.response.PageMetadata;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/wishlist")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Catalog - Wishlist", description = "Buyer saved-products list")
public class WishlistController {

    private final WishlistService wishlistService;
    private final ProductDtoMapper productDtoMapper;

    @GetMapping
    @Operation(summary = "List my saved products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> list(
            @CurrentOwnerId String userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        WishlistService.WishlistPage result = wishlistService.list(userId, page, size);
        return ResponseEntity.ok(ApiResponse.successWithPaging(
                productDtoMapper.toResponses(result.content()),
                PageMetadata.of(page, size, result.totalElements()),
                "Wishlist fetched"));
    }

    @PostMapping("/{productId}")
    @Operation(summary = "Save a product")
    public ResponseEntity<ApiResponse<Void>> add(
            @CurrentOwnerId String userId,
            @PathVariable String productId) {
        wishlistService.add(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product saved"));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove a saved product")
    public ResponseEntity<ApiResponse<Void>> remove(
            @CurrentOwnerId String userId,
            @PathVariable String productId) {
        wishlistService.remove(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist"));
    }

    @GetMapping("/{productId}/status")
    @Operation(summary = "Check whether a product is saved")
    public ResponseEntity<ApiResponse<WishlistStatusResponse>> status(
            @CurrentOwnerId String userId,
            @PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.success(
                new WishlistStatusResponse(wishlistService.isWishlisted(userId, productId)),
                "Wishlist status fetched"));
    }
}
