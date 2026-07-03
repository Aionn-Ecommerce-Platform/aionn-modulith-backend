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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(
                agentPersistencePort, agentAuditPort, agentPolicy, passwordHasher);
    }

    @Test
    void createSavesAgentWithExpectedFields() {
        when(agentPolicy.getKeyExpiryYears()).thenReturn(1);
        when(passwordHasher.hash(org.mockito.ArgumentMatchers.anyString())).thenReturn("hashed-key");
        when(agentPersistencePort.save(any(AgentIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentIdentity result = agentService.create(OWNER_ID);

        assertNotNull(result.getId());
        assertEquals(OWNER_ID, result.getOwnerId());
        assertEquals("hashed-key", result.getKeyHash());
        assertEquals(AgentStatus.ACTIVE, result.getStatus());
        assertTrue(result.getExpiresAt().isAfter(LocalDateTime.now().plusMonths(11)));
    }

    @Test
    void updatePermissionsRequiresOwnership() {
        when(agentPersistencePort.findByIdAndOwnerId(AGENT_ID, OWNER_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(IdentityException.class,
                () -> agentService.updatePermissions(OWNER_ID, AGENT_ID, "{}"));

        assertEquals(IdentityErrorCode.AGENT_NOT_FOUND.getCode(), ex.getErrorCode());
    }

    @Test
    void updatePermissionsPersistsNewJson() {
        AgentIdentity existing = baseAgent();
        when(agentPersistencePort.findByIdAndOwnerId(AGENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));
        when(agentPersistencePort.update(any(AgentIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentIdentity result = agentService.updatePermissions(OWNER_ID, AGENT_ID, "{\"scope\":\"full\"}");

        assertEquals("{\"scope\":\"full\"}", result.getPermissions());
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

        assertEquals(AgentStatus.SUSPENDED, result.getStatus());
        ArgumentCaptor<SecurityAudit> captor = ArgumentCaptor.forClass(SecurityAudit.class);
        verify(agentAuditPort).save(captor.capture());
        assertEquals(SecurityAuditEventType.AGENT_SUSPENDED, captor.getValue().getEventType());
        assertEquals(AGENT_ID, captor.getValue().getDeviceId());
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

        assertEquals(1, result.size());
    }

    @Test
    void getAgentAuditLogsRequiresOwnership() {
        when(agentPersistencePort.findByIdAndOwnerId(AGENT_ID, OWNER_ID)).thenReturn(Optional.empty());

        var ex = assertThrows(IdentityException.class,
                () -> agentService.getAgentAuditLogs(OWNER_ID, AGENT_ID));

        assertEquals(IdentityErrorCode.AGENT_NOT_FOUND.getCode(), ex.getErrorCode());
    }

    private AgentIdentity baseAgent() {
        return AgentIdentity.builder()
                .id(AGENT_ID)
                .ownerId(OWNER_ID)
                .name("Agent-test")
                .keyHash("hash")
                .permissions("{}")
                .status(AgentStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusYears(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
