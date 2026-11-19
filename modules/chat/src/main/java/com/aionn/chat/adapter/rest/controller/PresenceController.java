package com.aionn.chat.adapter.rest.controller;

import com.aionn.chat.adapter.rest.dto.presence.response.PresenceResponse;
import com.aionn.chat.application.port.out.PresenceTracker;
import com.aionn.platform.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
@Tag(name = "Chat - Presence", description = "Public online/last-seen status for store pages")
public class PresenceController {

    private final PresenceTracker presenceTracker;

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get public presence", description = "Online state and last-seen time for a user")
    public ResponseEntity<ApiResponse<PresenceResponse>> get(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                new PresenceResponse(
                        userId,
                        presenceTracker.isOnline(userId),
                        presenceTracker.lastSeenAt(userId).orElse(null)),
                "Presence fetched"));
    }
}
