package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.infrastructure.config.properties.NotificationPushProperties;
import com.aionn.platform.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/push")
@RequiredArgsConstructor
@Tag(name = "Notification - Push", description = "Web push subscription metadata")
public class NotificationPushController {

    private final NotificationPushProperties pushProperties;

    @GetMapping("/public-key")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the VAPID public key for browser push subscriptions")
    public ResponseEntity<ApiResponse<PushPublicKeyResponse>> publicKey() {
        String key = pushProperties.webPushPublicKey();
        return ResponseEntity.ok(ApiResponse.success(
                new PushPublicKeyResponse(key == null ? "" : key.trim()),
                "Push public key fetched"));
    }

    public record PushPublicKeyResponse(String publicKey) {
    }
}
