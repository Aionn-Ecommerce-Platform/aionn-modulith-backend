package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.merchant.response.FollowerCountResponse;
import com.aionn.catalog.adapter.rest.dto.merchant.response.MerchantStorefrontStatsResponse;
import com.aionn.catalog.adapter.rest.mapper.merchant.MerchantDtoMapper;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerId;
import com.aionn.catalog.adapter.rest.support.session.OptionalOwnerId;
import com.aionn.catalog.application.dto.merchant.result.MerchantStorefrontStatsResult;
import com.aionn.catalog.application.service.MerchantFollowService;
import com.aionn.catalog.application.service.MerchantStorefrontStatsService;
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
@RequestMapping("/api/v1/catalog/merchants/{merchantId}")
@RequiredArgsConstructor
@Tag(name = "Catalog - Merchant follow", description = "Merchant follow and storefront trust signals")
public class MerchantFollowController {

    private final MerchantFollowService followService;
    private final MerchantStorefrontStatsService statsService;
    private final MerchantDtoMapper merchantDtoMapper;

    @PostMapping("/follow")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Follow a merchant", description = "Record the authenticated buyer as a follower of the storefront")
    public ResponseEntity<ApiResponse<Void>> follow(
            @PathVariable String merchantId,
            @CurrentOwnerId String userId) {
        followService.follow(merchantId, userId);
        return ResponseEntity.ok(ApiResponse.success("Merchant followed"));
    }

    @DeleteMapping("/follow")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Unfollow a merchant", description = "Remove the authenticated buyer from the storefront followers")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @PathVariable String merchantId,
            @CurrentOwnerId String userId) {
        followService.unfollow(merchantId, userId);
        return ResponseEntity.ok(ApiResponse.success("Merchant unfollowed"));
    }

    @GetMapping("/followers/count")
    @Operation(summary = "Get the merchant follower count", description = "Public follower total shown on the storefront")
    public ResponseEntity<ApiResponse<FollowerCountResponse>> followerCount(
            @PathVariable String merchantId) {
        return ResponseEntity.ok(ApiResponse.success(
                new FollowerCountResponse(followService.followerCount(merchantId)),
                "Follower count fetched"));
    }

    @GetMapping("/storefront-stats")
    @Operation(summary = "Get merchant storefront trust signals",
            description = "Public rating, catalogue and chat response metrics; the following flag is personalised when authenticated")
    public ResponseEntity<ApiResponse<MerchantStorefrontStatsResponse>> stats(
            @PathVariable String merchantId,
            @OptionalOwnerId String userId) {
        MerchantStorefrontStatsResult result = statsService.stats(merchantId, userId);
        return ResponseEntity.ok(ApiResponse.success(
                merchantDtoMapper.toResponse(result), "Storefront stats fetched"));
    }
}
