package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.dto.subscription.RegisterDeviceTokenRequest;
import com.aionn.notification.adapter.rest.dto.subscription.UpdateSubscriptionRequest;
import com.aionn.notification.adapter.rest.dto.subscription.response.DeviceTokenResponse;
import com.aionn.notification.adapter.rest.dto.subscription.response.SubscriptionResponse;
import com.aionn.notification.adapter.rest.mapper.subscription.NotificationSubscriptionDtoMapper;
import com.aionn.notification.adapter.rest.support.session.CurrentUserId;
import com.aionn.notification.application.port.in.subscription.GetMySubscriptionInputPort;
import com.aionn.notification.application.port.in.subscription.ListMyDeviceTokensInputPort;
import com.aionn.notification.application.port.in.subscription.RegisterDeviceTokenInputPort;
import com.aionn.notification.application.port.in.subscription.RemoveDeviceTokenInputPort;
import com.aionn.notification.application.port.in.subscription.UpdateSubscriptionChannelInputPort;
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

        private final GetMySubscriptionInputPort getMySubscriptionInputPort;
        private final UpdateSubscriptionChannelInputPort updateSubscriptionChannelInputPort;
        private final RegisterDeviceTokenInputPort registerDeviceTokenInputPort;
        private final RemoveDeviceTokenInputPort removeDeviceTokenInputPort;
        private final ListMyDeviceTokensInputPort listMyDeviceTokensInputPort;
        private final NotificationSubscriptionDtoMapper dtoMapper;

        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get my subscription")
        public ResponseEntity<ApiResponse<SubscriptionResponse>> getMine(@CurrentUserId String userId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(getMySubscriptionInputPort.execute(userId)),
                                "Subscription fetched"));
        }

        @PutMapping("/me")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Update channel")
        public ResponseEntity<ApiResponse<SubscriptionResponse>> updateChannel(
                        @CurrentUserId String userId,
                        @Valid @RequestBody UpdateSubscriptionRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(updateSubscriptionChannelInputPort.execute(
                                                dtoMapper.toUpdateChannelCommand(userId, request))),
                                "Subscription updated"));
        }

        @PostMapping("/me/device-tokens")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Register device token")
        public ResponseEntity<ApiResponse<DeviceTokenResponse>> registerDevice(
                        @CurrentUserId String userId,
                        @Valid @RequestBody RegisterDeviceTokenRequest request) {
                return ApiResponse.createdResponse("Device token registered",
                                dtoMapper.toResponse(registerDeviceTokenInputPort.execute(
                                                dtoMapper.toRegisterDeviceTokenCommand(userId, request))));
        }

        @DeleteMapping("/me/device-tokens/{tokenId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Remove device token")
        public ResponseEntity<Void> removeDevice(
                        @CurrentUserId String userId,
                        @PathVariable String tokenId) {
                removeDeviceTokenInputPort.execute(dtoMapper.toRemoveDeviceTokenCommand(userId, tokenId));
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/me/device-tokens")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "List my device tokens")
        public ResponseEntity<ApiResponse<List<DeviceTokenResponse>>> listDevices(
                        @CurrentUserId String userId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toDeviceTokenResponses(listMyDeviceTokensInputPort.execute(userId)),
                                "Device tokens fetched"));
        }
}
