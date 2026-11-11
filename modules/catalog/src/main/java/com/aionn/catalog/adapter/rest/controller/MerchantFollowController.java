package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.merchant.response.MerchantStorefrontStatsResponse;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerId;
import com.aionn.catalog.application.dto.merchant.result.MerchantStorefrontStatsResult;
import com.aionn.catalog.application.service.MerchantFollowService;
import com.aionn.catalog.application.service.MerchantStorefrontStatsService;
import com.aionn.platform.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog/merchants/{merchantId}")
@RequiredArgsConstructor
public class MerchantFollowController {

    private final MerchantFollowService followService;
    private final MerchantStorefrontStatsService statsService;

    @PostMapping("/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> follow(
            @PathVariable String merchantId,
            @CurrentOwnerId String userId) {
        followService.follow(merchantId, userId);
        return ResponseEntity.ok(ApiResponse.success("Merchant followed"));
    }

    @DeleteMapping("/follow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @PathVariable String merchantId,
            @CurrentOwnerId String userId) {
        followService.unfollow(merchantId, userId);
        return ResponseEntity.ok(ApiResponse.success("Merchant unfollowed"));
    }

    @GetMapping("/followers/count")
    public ResponseEntity<ApiResponse<FollowerCountResponse>> followerCount(
            @PathVariable String merchantId) {
        return ResponseEntity.ok(ApiResponse.success(
                new FollowerCountResponse(followService.followerCount(merchantId)),
                "Follower count fetched"));
    }

    @GetMapping("/storefront-stats")
    public ResponseEntity<ApiResponse<MerchantStorefrontStatsResponse>> stats(
            @PathVariable String merchantId,
            Authentication authentication) {
        String userId = authenticatedUserId(authentication);
        MerchantStorefrontStatsResult result = statsService.stats(merchantId, userId);
        return ResponseEntity.ok(ApiResponse.success(toResponse(result), "Storefront stats fetched"));
    }

    private static String authenticatedUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }

    private static MerchantStorefrontStatsResponse toResponse(MerchantStorefrontStatsResult result) {
        return new MerchantStorefrontStatsResponse(
                result.reviewCount(),
                result.averageRating(),
                result.productCount(),
                result.followerCount(),
                result.joinedAt(),
                result.conversations(),
                result.respondedConversations(),
                result.medianFirstResponseSeconds(),
                result.responseRatePercent(),
                result.responseTimeBucket(),
                result.following());
    }

    public record FollowerCountResponse(long count) {
    }
}
