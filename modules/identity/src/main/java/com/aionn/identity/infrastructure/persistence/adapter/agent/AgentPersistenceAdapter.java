package com.aionn.identity.infrastructure.persistence.adapter.agent;

import com.aionn.identity.application.port.out.agent.AgentPersistencePort;
import com.aionn.identity.domain.model.AgentIdentity;
import com.aionn.identity.infrastructure.persistence.entity.AgentIdentityEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.AgentIdentityDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.agent.AgentIdentityRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AgentPersistenceAdapter implements AgentPersistencePort {

    private final AgentIdentityRepository agentIdentityRepository;
    private final UserRepository userRepository;
    private final AgentIdentityDomainMapper mapper;

    @Override
    public AgentIdentity save(AgentIdentity agentIdentity) {
        AgentIdentityEntity entity = mapper.toEntity(agentIdentity);
        UserEntity owner = userRepository.getReferenceById(agentIdentity.getOwnerId());
        entity.setOwner(owner);
        AgentIdentityEntity saved = agentIdentityRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AgentIdentity> findById(String agentId) {
        return agentIdentityRepository.findById(agentId)
                .map(mapper::toDomain);
    }

    @Override
    public List<AgentIdentity> findByOwnerId(String ownerId) {
        return agentIdentityRepository.findByOwner_UserIdOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<AgentIdentity> findByKeyHash(String keyHash) {
        return agentIdentityRepository.findByKeyHash(keyHash)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<AgentIdentity> findByIdAndOwnerId(String agentId, String ownerId) {
        return agentIdentityRepository.findByAgentIdAndOwner_UserId(agentId, ownerId)
                .map(mapper::toDomain);
    }

    @Override
    public AgentIdentity update(AgentIdentity agentIdentity) {
        AgentIdentityEntity entity = mapper.toEntity(agentIdentity);
        UserEntity owner = userRepository.getReferenceById(agentIdentity.getOwnerId());
        entity.setOwner(owner);
        AgentIdentityEntity updated = agentIdentityRepository.save(entity);
        return mapper.toDomain(updated);
    }

    @Override
    public void deleteByIdAndOwnerId(String agentId, String ownerId) {
        agentIdentityRepository.findByAgentIdAndOwner_UserId(agentId, ownerId)
                .ifPresent(agentIdentityRepository::delete);
    }
}
