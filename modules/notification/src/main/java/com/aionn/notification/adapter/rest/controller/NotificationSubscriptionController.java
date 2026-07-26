package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.dto.subscription.RegisterDeviceTokenRequest;
import com.aionn.notification.adapter.rest.dto.subscription.UpdateSubscriptionRequest;
import com.aionn.notification.adapter.rest.support.session.CurrentUserId;
import com.aionn.notification.application.dto.subscription.command.SubscriptionCommands;
import com.aionn.notification.application.dto.subscription.result.DeviceTokenResult;
import com.aionn.notification.application.dto.subscription.result.SubscriptionResult;
import com.aionn.notification.application.service.NotificationSubscriptionService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Notification - Subscription", description = "Per-user subscription / device tokens")
public class NotificationSubscriptionController {

    private final NotificationSubscriptionService subscriptionService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my subscription")
    public ResponseEntity<ApiResponse<SubscriptionResult>> getMine(@CurrentUserId String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                subscriptionService.get(userId), "Subscription fetched"));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update channel", description = "UC8.9")
    public ResponseEntity<ApiResponse<SubscriptionResult>> updateChannel(
            @CurrentUserId String userId,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                subscriptionService.updateChannel(new SubscriptionCommands.UpdateChannel(
                        userId, request.category(), request.channel(), request.enabled())),
                "Subscription updated"));
    }

    @PostMapping("/me/device-tokens")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Register device token", description = "UC8.8")
    public ResponseEntity<ApiResponse<DeviceTokenResult>> registerDevice(
            @CurrentUserId String userId,
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        return ApiResponse.createdResponse("Device token registered",
                subscriptionService.registerDeviceToken(new SubscriptionCommands.RegisterDeviceToken(
                        userId, request.deviceToken(), request.os())));
    }

    @DeleteMapping("/me/device-tokens/{tokenId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove device token")
    public ResponseEntity<Void> removeDevice(
            @CurrentUserId String userId,
            @PathVariable String tokenId) {
        subscriptionService.removeDeviceToken(new SubscriptionCommands.RemoveDeviceToken(
                userId, tokenId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/device-tokens")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my device tokens")
    public ResponseEntity<ApiResponse<List<DeviceTokenResult>>> listDevices(@CurrentUserId String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                subscriptionService.listDeviceTokens(userId), "Device tokens fetched"));
    }
}
