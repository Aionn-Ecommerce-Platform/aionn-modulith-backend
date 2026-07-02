package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.agent.request.CreateAgentIdentityRequest;
import com.aionn.identity.adapter.rest.dto.agent.request.UpdateAgentPermissionsRequest;
import com.aionn.identity.adapter.rest.dto.agent.response.AgentAuditLogResponse;
import com.aionn.identity.adapter.rest.dto.agent.response.AgentIdentityResponse;
import com.aionn.identity.adapter.rest.mapper.agent.AgentIdentityDtoMapper;
import com.aionn.identity.application.port.in.agent.*;
import com.aionn.sharedkernel.adapter.web.response.ApiResponse;
import com.aionn.sharedkernel.adapter.web.support.idempotency.IdempotentRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent-identities")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Identity - Agent Identity", description = "Identity module: AI agent key and permission management endpoints")
public class AgentIdentityController {

        private final ListMyAgentIdentitiesQueryPort listMyAgentIdentitiesQueryPort;
        private final GetAgentIdentityQueryPort getAgentIdentityQueryPort;
        private final CreateAgentIdentityInputPort createAgentIdentityInputPort;
        private final UpdateAgentPermissionsInputPort updateAgentPermissionsInputPort;
        private final SuspendAgentInputPort suspendAgentInputPort;
        private final GetAgentAuditLogsQueryPort getAgentAuditLogsQueryPort;
        private final RevokeAgentInputPort revokeAgentInputPort;
        private final AgentIdentityDtoMapper agentIdentityDtoMapper;

        @GetMapping
        @Operation(summary = "List agent identities", description = "Get all AI agent identities owned by the authenticated user")
        public ResponseEntity<ApiResponse<List<AgentIdentityResponse>>> listMyAgents(Authentication authentication) {
                var result = listMyAgentIdentitiesQueryPort.execute(authentication.getName());
                var response = agentIdentityDtoMapper.toResponses(result);
                return ResponseEntity.ok(ApiResponse.success(response, "Agent identities fetched"));
        }

        @GetMapping("/{agentId}")
        @Operation(summary = "Get agent identity", description = "Get detail of one AI agent identity by agent ID")
        public ResponseEntity<ApiResponse<AgentIdentityResponse>> getAgent(
                        Authentication authentication,
                        @PathVariable String agentId) {
                var result = getAgentIdentityQueryPort
                                .execute(agentIdentityDtoMapper.toGetAgentQuery(authentication.getName(), agentId));
                var response = agentIdentityDtoMapper.toResponse(result);
                return ResponseEntity.ok(ApiResponse.success(response, "Agent identity fetched"));
        }

        @PostMapping
        @IdempotentRequest(ttlSeconds = 300)
        @Operation(summary = "Create agent identity", description = "Create a new AI agent identity for the authenticated user")
        public ResponseEntity<ApiResponse<AgentIdentityResponse>> create(
                        Authentication authentication,
                        @Valid @RequestBody CreateAgentIdentityRequest request) {
                var result = createAgentIdentityInputPort.execute(
                                agentIdentityDtoMapper.toCreateCommand(authentication.getName(), request));
                var response = agentIdentityDtoMapper.toResponse(result);
                return ResponseEntity.ok(ApiResponse.success(response, "Agent identity created"));
        }

        @PutMapping("/{agentId}/permissions")
        @Operation(summary = "Update agent permissions", description = "Update permissions for an existing AI agent identity")
        public ResponseEntity<ApiResponse<AgentIdentityResponse>> updatePermissions(
                        Authentication authentication,
                        @PathVariable String agentId,
                        @Valid @RequestBody UpdateAgentPermissionsRequest request) {
                var result = updateAgentPermissionsInputPort.execute(agentIdentityDtoMapper
                                .toUpdatePermissionsCommand(authentication.getName(), agentId, request));
                var response = agentIdentityDtoMapper.toResponse(result);
                return ResponseEntity.ok(ApiResponse.success(response, "Agent permissions updated"));
        }

        @PostMapping("/{agentId}/suspend")
        @Operation(summary = "Suspend agent identity", description = "Suspend an AI agent identity by agent ID")
        public ResponseEntity<ApiResponse<AgentIdentityResponse>> suspend(
                        Authentication authentication,
                        @PathVariable String agentId) {
                var result = suspendAgentInputPort
                                .execute(agentIdentityDtoMapper.toSuspendCommand(authentication.getName(), agentId));
                var response = agentIdentityDtoMapper.toResponse(result);
                return ResponseEntity.ok(ApiResponse.success(response, "Agent suspended"));
        }

        @GetMapping("/{agentId}/audit-logs")
        @Operation(summary = "Get agent audit logs", description = "Get audit logs for a specific AI agent identity")
        public ResponseEntity<ApiResponse<List<AgentAuditLogResponse>>> auditLogs(
                        Authentication authentication,
                        @PathVariable String agentId) {
                var result = getAgentAuditLogsQueryPort
                                .execute(agentIdentityDtoMapper.toGetAuditLogsQuery(authentication.getName(), agentId));
                var response = agentIdentityDtoMapper.toAuditLogResponses(result);
                return ResponseEntity.ok(ApiResponse.success(response, "Agent audit logs"));
        }

        @DeleteMapping("/{agentId}")
        @Operation(summary = "Revoke agent identity", description = "Revoke and delete an AI agent identity by agent ID")
        public ResponseEntity<ApiResponse<Void>> revoke(
                        Authentication authentication,
                        @PathVariable String agentId) {
                revokeAgentInputPort.execute(agentIdentityDtoMapper.toRevokeCommand(authentication.getName(), agentId));
                return ResponseEntity.ok(ApiResponse.success("Agent identity revoked"));
        }
}
