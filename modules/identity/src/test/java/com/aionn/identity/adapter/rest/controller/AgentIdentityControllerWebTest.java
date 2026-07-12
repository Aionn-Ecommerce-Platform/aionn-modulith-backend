package com.aionn.identity.adapter.rest.controller;

import com.aionn.identity.adapter.rest.dto.agent.request.CreateAgentIdentityRequest;
import com.aionn.identity.adapter.rest.dto.agent.request.UpdateAgentPermissionsRequest;
import com.aionn.identity.adapter.rest.dto.agent.response.AgentAuditLogResponse;
import com.aionn.identity.adapter.rest.dto.agent.response.AgentIdentityResponse;
import com.aionn.identity.adapter.rest.exception.IdentityExceptionHandler;
import com.aionn.identity.adapter.rest.mapper.agent.AgentIdentityDtoMapper;
import com.aionn.identity.adapter.rest.support.MockAuthenticationArgumentResolver;
import com.aionn.identity.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.identity.application.dto.agent.command.CreateAgentIdentityCommand;
import com.aionn.identity.application.dto.agent.command.RevokeAgentCommand;
import com.aionn.identity.application.dto.agent.command.SuspendAgentCommand;
import com.aionn.identity.application.dto.agent.command.UpdateAgentPermissionsCommand;
import com.aionn.identity.application.dto.agent.query.GetAgentAuditLogsQuery;
import com.aionn.identity.application.dto.agent.query.GetAgentIdentityQuery;
import com.aionn.identity.application.dto.agent.result.AgentAuditLogResult;
import com.aionn.identity.application.dto.agent.result.AgentIdentityResult;
import com.aionn.identity.application.port.in.agent.CreateAgentIdentityInputPort;
import com.aionn.identity.application.port.in.agent.GetAgentAuditLogsQueryPort;
import com.aionn.identity.application.port.in.agent.GetAgentIdentityQueryPort;
import com.aionn.identity.application.port.in.agent.ListMyAgentIdentitiesQueryPort;
import com.aionn.identity.application.port.in.agent.RevokeAgentInputPort;
import com.aionn.identity.application.port.in.agent.SuspendAgentInputPort;
import com.aionn.identity.application.port.in.agent.UpdateAgentPermissionsInputPort;
import com.aionn.sharedkernel.adapter.web.support.clientip.ClientIpArgumentResolver;
import com.aionn.sharedkernel.infrastructure.web.ClientIpResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web tests for AgentIdentityController. Covers list/get for the authenticated
 * user, create with idempotent semantics, update-permissions, suspend/revoke,
 * audit logs, and authentication enforcement.
 */
@ExtendWith(MockitoExtension.class)
class AgentIdentityControllerWebTest {

    @Mock
    private ListMyAgentIdentitiesQueryPort listMyAgentIdentitiesQueryPort;
    @Mock
    private GetAgentIdentityQueryPort getAgentIdentityQueryPort;
    @Mock
    private CreateAgentIdentityInputPort createAgentIdentityInputPort;
    @Mock
    private UpdateAgentPermissionsInputPort updateAgentPermissionsInputPort;
    @Mock
    private SuspendAgentInputPort suspendAgentInputPort;
    @Mock
    private GetAgentAuditLogsQueryPort getAgentAuditLogsQueryPort;
    @Mock
    private RevokeAgentInputPort revokeAgentInputPort;
    @Mock
    private AgentIdentityDtoMapper agentIdentityDtoMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AgentIdentityController controller = new AgentIdentityController(
                listMyAgentIdentitiesQueryPort, getAgentIdentityQueryPort, createAgentIdentityInputPort,
                updateAgentPermissionsInputPort, suspendAgentInputPort, getAgentAuditLogsQueryPort,
                revokeAgentInputPort, agentIdentityDtoMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new IdentityExceptionHandler())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setCustomArgumentResolvers(
                        new ClientIpArgumentResolver(new ClientIpResolver()),
                        new MockAuthenticationArgumentResolver())
                .build();
    }

    private AgentIdentityResult sampleResult(String agentId) {
        Instant now = Instant.now();
        return new AgentIdentityResult(
                agentId,
                "hash-" + agentId,
                "{\"scopes\":[\"orders:read\"]}",
                "ACTIVE",
                now.plus(Duration.ofDays(30)),
                now);
    }

    private AgentIdentityResponse sampleResponse(String agentId) {
        Instant now = Instant.now();
        return new AgentIdentityResponse(
                agentId,
                "hash-" + agentId,
                "{\"scopes\":[\"orders:read\"]}",
                "ACTIVE",
                now.plus(Duration.ofDays(30)),
                now);
    }

    @Test
    void listMyAgentsReturnsAgentsOwnedByCurrentUser() throws Exception {
        List<AgentIdentityResult> results = List.of(sampleResult("agent-1"), sampleResult("agent-2"));
        List<AgentIdentityResponse> responses = List.of(sampleResponse("agent-1"), sampleResponse("agent-2"));

        when(listMyAgentIdentitiesQueryPort.execute("alice@example.com")).thenReturn(results);
        when(agentIdentityDtoMapper.toResponses(results)).thenReturn(responses);

        mockMvc.perform(get("/api/v1/agent-identities")
                .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].agentId").value("agent-1"))
                .andExpect(jsonPath("$.data[1].agentId").value("agent-2"));

        verify(listMyAgentIdentitiesQueryPort).execute("alice@example.com");
    }

    @Test
    void getAgentReturnsAgentDetail() throws Exception {
        AgentIdentityResult result = sampleResult("agent-1");
        AgentIdentityResponse response = sampleResponse("agent-1");
        GetAgentIdentityQuery query = new GetAgentIdentityQuery("alice@example.com", "agent-1");

        when(agentIdentityDtoMapper.toGetAgentQuery("alice@example.com", "agent-1")).thenReturn(query);
        when(getAgentIdentityQueryPort.execute(query)).thenReturn(result);
        when(agentIdentityDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(get("/api/v1/agent-identities/agent-1")
                .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentId").value("agent-1"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(getAgentIdentityQueryPort).execute(query);
    }

    @Test
    void createCreatesAgentForCurrentUser() throws Exception {
        AgentIdentityResult result = sampleResult("agent-new");
        AgentIdentityResponse response = sampleResponse("agent-new");
        CreateAgentIdentityCommand command = new CreateAgentIdentityCommand(
                "alice@example.com", "Shopping Assistant");

        when(agentIdentityDtoMapper.toCreateCommand(eq("alice@example.com"),
                any(CreateAgentIdentityRequest.class))).thenReturn(command);
        when(createAgentIdentityInputPort.execute(command)).thenReturn(result);
        when(agentIdentityDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/agent-identities")
                .with(user("alice@example.com").roles("USER"))
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "agentName": "Shopping Assistant"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agentId").value("agent-new"))
                .andExpect(jsonPath("$.message").value("Agent identity created"));

        verify(createAgentIdentityInputPort).execute(command);
    }

    @Test
    void createRejectsBlankAgentName() throws Exception {
        mockMvc.perform(post("/api/v1/agent-identities")
                .with(user("alice@example.com").roles("USER"))
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "agentName": ""
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createAgentIdentityInputPort);
    }

    @Test
    void updatePermissionsRevisesAgentScopes() throws Exception {
        AgentIdentityResult result = sampleResult("agent-1");
        AgentIdentityResponse response = sampleResponse("agent-1");
        UpdateAgentPermissionsCommand command = new UpdateAgentPermissionsCommand(
                "alice@example.com", "agent-1", "{\"scopes\":[\"orders:read\",\"cart:write\"]}");

        when(agentIdentityDtoMapper.toUpdatePermissionsCommand(eq("alice@example.com"),
                eq("agent-1"), any(UpdateAgentPermissionsRequest.class))).thenReturn(command);
        when(updateAgentPermissionsInputPort.execute(command)).thenReturn(result);
        when(agentIdentityDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(put("/api/v1/agent-identities/agent-1/permissions")
                .with(user("alice@example.com").roles("USER"))
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "permissionsJson": "{\\"scopes\\":[\\"orders:read\\",\\"cart:write\\"]}"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Agent permissions updated"));

        verify(updateAgentPermissionsInputPort).execute(command);
    }

    @Test
    void updatePermissionsRejectsBlankPermissionsJson() throws Exception {
        mockMvc.perform(put("/api/v1/agent-identities/agent-1/permissions")
                .with(user("alice@example.com").roles("USER"))
                .contentType(APPLICATION_JSON)
                .content("""
                        {
                          "permissionsJson": ""
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(updateAgentPermissionsInputPort);
    }

    @Test
    void suspendSuspendsAgent() throws Exception {
        AgentIdentityResult result = new AgentIdentityResult(
                "agent-1", "hash-x", "{}", "SUSPENDED",
                Instant.now().plus(Duration.ofDays(30)), Instant.now());
        AgentIdentityResponse response = new AgentIdentityResponse(
                "agent-1", "hash-x", "{}", "SUSPENDED",
                Instant.now().plus(Duration.ofDays(30)), Instant.now());
        SuspendAgentCommand command = new SuspendAgentCommand("alice@example.com", "agent-1");

        when(agentIdentityDtoMapper.toSuspendCommand("alice@example.com", "agent-1")).thenReturn(command);
        when(suspendAgentInputPort.execute(command)).thenReturn(result);
        when(agentIdentityDtoMapper.toResponse(result)).thenReturn(response);

        mockMvc.perform(post("/api/v1/agent-identities/agent-1/suspend")
                .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.message").value("Agent suspended"));

        verify(suspendAgentInputPort).execute(command);
    }

    @Test
    void auditLogsReturnsAgentAuditTrail() throws Exception {
        Instant now = Instant.now();
        List<AgentAuditLogResult> results = List.of(
                new AgentAuditLogResult("audit-1", "AGENT_CREATED",
                        "Agent provisioned", "192.168.1.50", "device-abc", now.minus(Duration.ofHours(2))),
                new AgentAuditLogResult("audit-2", "PERMISSIONS_UPDATED",
                        "Scopes changed", "192.168.1.50", "device-abc", now.minus(Duration.ofMinutes(10))));
        List<AgentAuditLogResponse> responses = List.of(
                new AgentAuditLogResponse("audit-1", "AGENT_CREATED",
                        "Agent provisioned", "192.168.1.50", "device-abc", now.minus(Duration.ofHours(2))),
                new AgentAuditLogResponse("audit-2", "PERMISSIONS_UPDATED",
                        "Scopes changed", "192.168.1.50", "device-abc", now.minus(Duration.ofMinutes(10))));
        GetAgentAuditLogsQuery query = new GetAgentAuditLogsQuery("alice@example.com", "agent-1");

        when(agentIdentityDtoMapper.toGetAuditLogsQuery("alice@example.com", "agent-1")).thenReturn(query);
        when(getAgentAuditLogsQueryPort.execute(query)).thenReturn(results);
        when(agentIdentityDtoMapper.toAuditLogResponses(results)).thenReturn(responses);

        mockMvc.perform(get("/api/v1/agent-identities/agent-1/audit-logs")
                .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].eventType").value("AGENT_CREATED"))
                .andExpect(jsonPath("$.data[1].eventType").value("PERMISSIONS_UPDATED"));

        verify(getAgentAuditLogsQueryPort).execute(query);
    }

    @Test
    void revokeRevokesAgent() throws Exception {
        RevokeAgentCommand command = new RevokeAgentCommand("alice@example.com", "agent-1");

        when(agentIdentityDtoMapper.toRevokeCommand("alice@example.com", "agent-1")).thenReturn(command);
        doNothing().when(revokeAgentInputPort).execute(command);

        mockMvc.perform(delete("/api/v1/agent-identities/agent-1")
                .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Agent identity revoked"));

        verify(revokeAgentInputPort).execute(command);
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/agent-identities"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(listMyAgentIdentitiesQueryPort);
    }
}
