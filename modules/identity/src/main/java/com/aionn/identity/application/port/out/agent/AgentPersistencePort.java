package com.aionn.identity.application.port.out.agent;

import com.aionn.identity.domain.model.AgentIdentity;

import java.util.List;
import java.util.Optional;

public interface AgentPersistencePort {

    AgentIdentity save(AgentIdentity agentIdentity);

    Optional<AgentIdentity> findById(String agentId);

    List<AgentIdentity> findByOwnerId(String ownerId);

    Optional<AgentIdentity> findByKeyHash(String keyHash);

    Optional<AgentIdentity> findByIdAndOwnerId(String agentId, String ownerId);

    AgentIdentity update(AgentIdentity agentIdentity);

    void delete(String agentId);
}
