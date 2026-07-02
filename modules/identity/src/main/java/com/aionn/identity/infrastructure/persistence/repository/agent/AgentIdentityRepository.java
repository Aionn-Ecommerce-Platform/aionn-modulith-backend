package com.aionn.identity.infrastructure.persistence.repository.agent;

import com.aionn.identity.infrastructure.persistence.entity.AgentIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentIdentityRepository extends JpaRepository<AgentIdentityEntity, String> {

    List<AgentIdentityEntity> findByOwner_UserIdOrderByCreatedAtDesc(String ownerUserId);

    Optional<AgentIdentityEntity> findByKeyHash(String keyHash);

    Optional<AgentIdentityEntity> findByAgentIdAndOwner_UserId(String agentId, String ownerUserId);
}


