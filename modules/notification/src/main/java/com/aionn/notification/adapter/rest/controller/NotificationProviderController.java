package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.dto.provider.ConfigureProviderRequest;
import com.aionn.notification.adapter.rest.dto.provider.UpdateProviderRequest;
import com.aionn.notification.adapter.rest.dto.provider.response.ProviderResponse;
import com.aionn.notification.adapter.rest.mapper.provider.NotificationProviderDtoMapper;
import com.aionn.notification.adapter.rest.support.session.CurrentAdminId;
import com.aionn.notification.application.dto.analytics.result.AnalyticsResult;
import com.aionn.notification.application.port.in.analytics.GetCampaignAnalyticsInputPort;
import com.aionn.notification.application.port.in.provider.ConfigureProviderInputPort;
import com.aionn.notification.application.port.in.provider.ListProvidersInputPort;
import com.aionn.notification.application.port.in.provider.UpdateProviderInputPort;
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

        private final ConfigureProviderInputPort configureProviderInputPort;
        private final UpdateProviderInputPort updateProviderInputPort;
        private final ListProvidersInputPort listProvidersInputPort;
        private final GetCampaignAnalyticsInputPort getCampaignAnalyticsInputPort;
        private final NotificationProviderDtoMapper dtoMapper;

        @PostMapping("/providers")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Configure provider")
        public ResponseEntity<ApiResponse<ProviderResponse>> configure(
                        @CurrentAdminId String adminId,
                        @Valid @RequestBody ConfigureProviderRequest request) {
                return ApiResponse.createdResponse("Provider configured",
                                dtoMapper.toResponse(configureProviderInputPort.execute(
                                                dtoMapper.toConfigureCommand(adminId, request))));
        }

        @PutMapping("/providers/{providerId}")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "Update provider")
        public ResponseEntity<ApiResponse<ProviderResponse>> update(
                        @CurrentAdminId String adminId,
                        @PathVariable String providerId,
                        @Valid @RequestBody UpdateProviderRequest request) {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponse(updateProviderInputPort.execute(
                                                dtoMapper.toUpdateCommand(providerId, adminId, request))),
                                "Provider updated"));
        }

        @GetMapping("/providers")
        @PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
        @Operation(summary = "List providers")
        public ResponseEntity<ApiResponse<List<ProviderResponse>>> list() {
                return ResponseEntity.ok(ApiResponse.success(
                                dtoMapper.toResponses(listProvidersInputPort.execute()),
                                "Providers fetched"));
        }

        @GetMapping("/analytics")
        @PreAuthorize("hasAnyAuthority('ROLE_SYSTEM_ADMIN','ROLE_CS_ADMIN')")
        @Operation(summary = "Campaign analytics")
        public ResponseEntity<ApiResponse<AnalyticsResult>> analytics(@RequestParam String campaignId) {
                return ResponseEntity.ok(ApiResponse.success(
                                getCampaignAnalyticsInputPort.execute(campaignId), "Analytics generated"));
        }
}
