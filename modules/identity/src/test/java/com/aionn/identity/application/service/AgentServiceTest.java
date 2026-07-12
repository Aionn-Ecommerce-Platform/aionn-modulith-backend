package com.aionn.identity.application.service;

import com.aionn.identity.application.policy.AgentPolicy;
import com.aionn.identity.application.port.out.agent.AgentAuditPort;
import com.aionn.identity.application.port.out.agent.AgentPersistencePort;
import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.AgentIdentity;
import com.aionn.identity.domain.model.SecurityAudit;
import com.aionn.identity.domain.valueobject.AgentStatus;
import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    private static final String OWNER_ID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
    private static final String AGENT_ID = "01HZAGENT0000000000000000";

    @Mock
    private AgentPersistencePort agentPersistencePort;
    @Mock
    private AgentAuditPort agentAuditPort;
    @Mock
    private AgentPolicy agentPolicy;
    @Mock
    private PasswordHasherPort passwordHasher;

    private static final Instant FIXED_NOW = Instant.parse("2026-07-12T10:00:00Z");

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(
                agentPersistencePort, agentAuditPort, agentPolicy, passwordHasher,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void createSavesAgentWithExpectedFields() {
        when(agentPolicy.getKeyExpiryYears()).thenReturn(1);
        when(passwordHasher.hash(org.mockito.ArgumentMatchers.anyString())).thenReturn("hashed-key");
        when(agentPersistencePort.save(any(AgentIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentIdentity result = agentService.create(OWNER_ID);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(result.getKeyHash()).isEqualTo("hashed-key");
        assertThat(result.getStatus()).isEqualTo(AgentStatus.ACTIVE);
        Instant expectedExpiry = FIXED_NOW.atZone(ZoneOffset.UTC).plusYears(1).toInstant();
        assertThat(result.getExpiresAt()).isEqualTo(expectedExpiry);
    }

    @Test
    void updatePermissionsRequiresOwnership() {
        when(agentPersistencePort.findByIdAndOwnerId(AGENT_ID, OWNER_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(IdentityException.class,
                () -> agentService.updatePermissions(OWNER_ID, AGENT_ID, "{}"));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.AGENT_NOT_FOUND.getCode());
    }

    @Test
    void updatePermissionsPersistsNewJson() {
        AgentIdentity existing = baseAgent();
        when(agentPersistencePort.findByIdAndOwnerId(AGENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));
        when(agentPersistencePort.update(any(AgentIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentIdentity result = agentService.updatePermissions(OWNER_ID, AGENT_ID, "{\"scope\":\"full\"}");

        assertThat(result.getPermissions()).isEqualTo("{\"scope\":\"full\"}");
    }

    @Test
    void suspendUpdatesStatusAndAudits() {
        AgentIdentity existing = baseAgent();
        when(agentPersistencePort.findByIdAndOwnerId(AGENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));
        when(agentPersistencePort.update(any(AgentIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(agentAuditPort.save(any(SecurityAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentIdentity result = agentService.suspend(OWNER_ID, AGENT_ID);

        assertThat(result.getStatus()).isEqualTo(AgentStatus.SUSPENDED);
        ArgumentCaptor<SecurityAudit> captor = ArgumentCaptor.forClass(SecurityAudit.class);
        verify(agentAuditPort).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(SecurityAuditEventType.AGENT_SUSPENDED);
        assertThat(captor.getValue().getDeviceId()).isEqualTo(AGENT_ID);
    }

    @Test
    void revokeRemovesAgent() {
        AgentIdentity existing = baseAgent();
        when(agentPersistencePort.findByIdAndOwnerId(AGENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));

        agentService.revoke(OWNER_ID, AGENT_ID);

        verify(agentPersistencePort).deleteByIdAndOwnerId(AGENT_ID, OWNER_ID);
    }

    @Test
    void listMyDelegatesToPort() {
        when(agentPersistencePort.findByOwnerId(OWNER_ID)).thenReturn(List.of(baseAgent()));

        List<AgentIdentity> result = agentService.listMy(OWNER_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void getAgentAuditLogsRequiresOwnership() {
        when(agentPersistencePort.findByIdAndOwnerId(AGENT_ID, OWNER_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(IdentityException.class,
                () -> agentService.getAgentAuditLogs(OWNER_ID, AGENT_ID));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.AGENT_NOT_FOUND.getCode());
    }

    private AgentIdentity baseAgent() {
        return AgentIdentity.builder()
                .id(AGENT_ID)
                .ownerId(OWNER_ID)
                .name("Agent-test")
                .keyHash("hash")
                .permissions("{}")
                .status(AgentStatus.ACTIVE)
                .expiresAt(Instant.now().atZone(ZoneOffset.UTC).plusYears(1).toInstant())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
