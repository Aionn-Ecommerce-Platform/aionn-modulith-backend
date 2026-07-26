package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.dto.provider.ConfigureProviderRequest;
import com.aionn.notification.adapter.rest.dto.provider.UpdateProviderRequest;
import com.aionn.notification.adapter.rest.support.session.CurrentAdminId;
import com.aionn.notification.application.dto.provider.command.ProviderCommands;
import com.aionn.notification.application.dto.provider.result.ProviderResult;
import com.aionn.notification.application.service.NotificationAnalyticsService;
import com.aionn.notification.application.service.NotificationProviderService;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification - Admin", description = "Provider config + analytics")
public class NotificationProviderController {

    private final NotificationProviderService providerService;
    private final NotificationAnalyticsService analyticsService;

    @PostMapping("/providers")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Configure provider", description = "UC8.11")
    public ResponseEntity<ApiResponse<ProviderResult>> configure(
            @CurrentAdminId String adminId,
            @Valid @RequestBody ConfigureProviderRequest request) {
        return ApiResponse.createdResponse("Provider configured",
                providerService.configure(new ProviderCommands.ConfigureProvider(
                        request.channel(), request.providerType(), request.config(),
                        request.rateLimitPerMinute(), adminId)));
    }

    @PutMapping("/providers/{providerId}")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "Update provider")
    public ResponseEntity<ApiResponse<ProviderResult>> update(
            @CurrentAdminId String adminId,
            @PathVariable String providerId,
            @Valid @RequestBody UpdateProviderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                providerService.update(new ProviderCommands.UpdateProvider(
                        providerId, request.config(), request.rateLimitPerMinute(),
                        request.active(), adminId)),
                "Provider updated"));
    }

    @GetMapping("/providers")
    @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
    @Operation(summary = "List providers")
    public ResponseEntity<ApiResponse<List<ProviderResult>>> list() {
        return ResponseEntity.ok(ApiResponse.success(providerService.listAll(), "Providers fetched"));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
    @Operation(summary = "Campaign analytics", description = "UC8.12")
    public ResponseEntity<ApiResponse<?>> analytics(@RequestParam String campaignId) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.report(campaignId), "Analytics generated"));
    }
}
