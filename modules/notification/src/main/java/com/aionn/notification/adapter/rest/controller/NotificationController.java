package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.dto.notification.SendNotificationRequest;
import com.aionn.notification.adapter.rest.support.session.CurrentUserId;
import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.dto.notification.result.NotificationResult;
import com.aionn.notification.application.service.NotificationDispatchService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "Notification dispatch + inbox endpoints")
public class NotificationController {

        private final NotificationDispatchService dispatchService;

        @PostMapping("/dispatch")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Send by event", description = "UC8.1 - system / admin trigger")
        public ResponseEntity<ApiResponse<List<NotificationResult>>> dispatch(
                        @Valid @RequestBody SendNotificationRequest request) {
                List<NotificationResult> result = dispatchService.sendByEvent(new NotificationCommands.SendByEvent(
                                request.userId(), request.eventType(), request.category(),
                                request.channels(), request.locale(), request.campaignId(), request.context()));
                return ResponseEntity.ok(ApiResponse.success(result, "Notifications dispatched"));
        }

        @PostMapping("/{notiId}/read")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Mark read", description = "UC8.4")
        public ResponseEntity<ApiResponse<NotificationResult>> markRead(
                        @CurrentUserId String userId,
                        @PathVariable String notiId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dispatchService.markRead(
                                                new NotificationCommands.MarkRead(userId, notiId)),
                                "Notification marked read"));
        }

        @DeleteMapping("/{notiId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Delete", description = "UC8.5 soft delete")
        public ResponseEntity<ApiResponse<NotificationResult>> delete(
                        @CurrentUserId String userId,
                        @PathVariable String notiId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dispatchService.delete(
                                                new NotificationCommands.MarkDeleted(userId, notiId)),
                                "Notification deleted"));
        }

        @GetMapping("/{notiId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get notification")
        public ResponseEntity<ApiResponse<NotificationResult>> get(
                        @CurrentUserId String userId,
                        @PathVariable String notiId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dispatchService.get(userId, notiId), "Notification fetched"));
        }

        @GetMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "List my notifications")
        public ResponseEntity<ApiResponse<List<NotificationResult>>> listMine(
                        @CurrentUserId String userId,
                        @RequestParam(defaultValue = "50") int limit) {
                int safeLimit = Math.min(Math.max(limit, 1), 100);
                return ResponseEntity.ok(ApiResponse.success(
                                dispatchService.listMine(userId, safeLimit),
                                "Notifications fetched"));
        }
}
