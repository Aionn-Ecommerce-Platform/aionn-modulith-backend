package com.aionn.identity.application.service;

import com.aionn.identity.application.policy.AgentPolicy;
import com.aionn.identity.application.policy.IdentityValidationConstants;
import com.aionn.identity.application.port.out.agent.AgentAuditPort;
import com.aionn.identity.application.port.out.agent.AgentPersistencePort;
import com.aionn.identity.application.port.out.security.PasswordHasherPort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.AgentIdentity;
import com.aionn.identity.domain.model.SecurityAudit;
import com.aionn.identity.domain.valueobject.AgentStatus;
import com.aionn.identity.domain.valueobject.SecurityAuditEventType;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

    private final AgentPersistencePort agentPersistencePort;
    private final AgentAuditPort agentAuditPort;
    private final AgentPolicy agentPolicy;
    private final PasswordHasherPort passwordHasher;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AgentIdentity create(String ownerUserId) {
        log.info("Creating agent for owner: {}", ownerUserId);
        byte[] keyBytes = new byte[IdentityValidationConstants.AGENT_KEY_BYTES];
        SECURE_RANDOM.nextBytes(keyBytes);
        String secureKey = Base64.getEncoder().encodeToString(keyBytes);
        String keyHash = passwordHasher.hash(secureKey);

        AgentIdentity agentIdentity = AgentIdentity.builder()
                .id(IdGenerator.ulid())
                .ownerId(ownerUserId)
                .name("Agent-" + IdGenerator.ulid().substring(0, 8))
                .keyHash(keyHash)
                .permissions("{\"scope\":\"basic\"}")
                .status(AgentStatus.ACTIVE)
                .expiresAt(nowUtc().plusYears(agentPolicy.getKeyExpiryYears()))
                .createdAt(nowUtc())
                .updatedAt(nowUtc())
                .build();

        AgentIdentity saved = agentPersistencePort.save(agentIdentity);
        log.info("Agent created successfully: {}", saved.getId());
        return saved;
    }

    public AgentIdentity updatePermissions(String ownerUserId, String agentId, String permissionsJson) {
        log.info("Updating permissions for agent: {} owned by: {}", agentId, ownerUserId);
        AgentIdentity agentIdentity = getOwnedAgent(ownerUserId, agentId);
        agentIdentity.updatePermissions(permissionsJson);
        AgentIdentity updated = agentPersistencePort.update(agentIdentity);
        log.info("Agent permissions updated successfully: {}", agentId);
        return updated;
    }

    public AgentIdentity suspend(String ownerUserId, String agentId) {
        log.info("Suspending agent: {} owned by: {}", agentId, ownerUserId);
        AgentIdentity agentIdentity = getOwnedAgent(ownerUserId, agentId);
        agentIdentity.suspend();
        AgentIdentity suspended = agentPersistencePort.update(agentIdentity);
        SecurityAudit audit = SecurityAudit.builder()
                .id(IdGenerator.ulid())
                .userId(ownerUserId)
                .eventType(SecurityAuditEventType.AGENT_SUSPENDED)
                .description("Agent suspended: " + agentId)
                .deviceId(agentId)
                .timestamp(nowUtc())
                .build();
        agentAuditPort.save(audit);

        log.info("Agent suspended successfully: {}", agentId);
        return suspended;
    }

    @Transactional(readOnly = true)
    public List<SecurityAudit> getAgentAuditLogs(String ownerUserId, String agentId) {
        log.debug("Retrieving audit logs for agent: {} owned by: {}", agentId, ownerUserId);
        getOwnedAgent(ownerUserId, agentId);
        return agentAuditPort.findByAgentId(agentId, 100);
    }

    @Transactional(readOnly = true)
    public List<AgentIdentity> listMy(String ownerUserId) {
        log.debug("Listing agents for owner: {}", ownerUserId);
        return agentPersistencePort.findByOwnerId(ownerUserId);
    }

    @Transactional(readOnly = true)
    public AgentIdentity get(String ownerUserId, String agentId) {
        log.debug("Getting agent: {} for owner: {}", agentId, ownerUserId);
        return getOwnedAgent(ownerUserId, agentId);
    }

    public void revoke(String ownerUserId, String agentId) {
        log.info("Revoking agent: {} owned by: {}", agentId, ownerUserId);
        AgentIdentity agentIdentity = getOwnedAgent(ownerUserId, agentId);
        agentPersistencePort.deleteByIdAndOwnerId(agentIdentity.getId(), ownerUserId);
        log.info("Agent revoked successfully: {}", agentId);
    }

    private AgentIdentity getOwnedAgent(String ownerUserId, String agentId) {
        return agentPersistencePort.findByIdAndOwnerId(agentId, ownerUserId)
                .orElseThrow(() -> new IdentityException(IdentityErrorCode.AGENT_NOT_FOUND, "Agent not found"));
    }

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
