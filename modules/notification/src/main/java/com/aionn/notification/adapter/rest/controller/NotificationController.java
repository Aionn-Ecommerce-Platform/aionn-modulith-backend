package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.dto.notification.SendNotificationRequest;
import com.aionn.notification.adapter.rest.dto.notification.response.NotificationResponse;
import com.aionn.notification.adapter.rest.mapper.notification.NotificationDtoMapper;
import com.aionn.notification.adapter.rest.support.session.CurrentUserId;
import com.aionn.notification.application.port.in.notification.DeleteNotificationInputPort;
import com.aionn.notification.application.port.in.notification.GetMyNotificationInputPort;
import com.aionn.notification.application.port.in.notification.ListMyNotificationsInputPort;
import com.aionn.notification.application.port.in.notification.MarkNotificationReadInputPort;
import com.aionn.notification.application.port.in.notification.SendNotificationByEventInputPort;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

        private final SendNotificationByEventInputPort sendNotificationByEventInputPort;
        private final MarkNotificationReadInputPort markNotificationReadInputPort;
        private final DeleteNotificationInputPort deleteNotificationInputPort;
        private final GetMyNotificationInputPort getMyNotificationInputPort;
        private final ListMyNotificationsInputPort listMyNotificationsInputPort;
        private final NotificationDtoMapper dtoMapper;

        @PostMapping("/dispatch")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Send by event", description = "System or administrator-triggered notification")
        public ResponseEntity<ApiResponse<List<NotificationResponse>>> dispatch(
                        @Valid @RequestBody SendNotificationRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponses(sendNotificationByEventInputPort.execute(
                                                dtoMapper.toSendByEventCommand(request))),
                                "Notifications dispatched"));
        }

        @PostMapping("/{notiId}/read")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Mark read")
        public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
                        @CurrentUserId String userId,
                        @PathVariable String notiId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(markNotificationReadInputPort.execute(
                                                dtoMapper.toMarkReadCommand(userId, notiId))),
                                "Notification marked read"));
        }

        @DeleteMapping("/{notiId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Delete", description = "Soft-delete a notification from the user's inbox")
        public ResponseEntity<ApiResponse<NotificationResponse>> delete(
                        @CurrentUserId String userId,
                        @PathVariable String notiId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(deleteNotificationInputPort.execute(
                                                dtoMapper.toMarkDeletedCommand(userId, notiId))),
                                "Notification deleted"));
        }

        @GetMapping("/{notiId}")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get notification")
        public ResponseEntity<ApiResponse<NotificationResponse>> get(
                        @CurrentUserId String userId,
                        @PathVariable String notiId) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(getMyNotificationInputPort.execute(userId, notiId)),
                                "Notification fetched"));
        }

        @GetMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "List my notifications")
        public ResponseEntity<ApiResponse<List<NotificationResponse>>> listMine(
                        @CurrentUserId String userId,
                        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponses(listMyNotificationsInputPort.execute(userId, limit)),
                                "Notifications fetched"));
        }
}
