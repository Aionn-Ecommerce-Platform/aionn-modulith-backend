package com.aionn.identity.infrastructure.persistence.adapter.agent;

import com.aionn.identity.application.port.out.agent.AgentAuditPort;
import com.aionn.identity.domain.model.SecurityAudit;
import com.aionn.identity.infrastructure.persistence.entity.SecurityAuditEntity;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.mapper.SecurityAuditDomainMapper;
import com.aionn.identity.infrastructure.persistence.repository.security.SecurityAuditRepository;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentAuditAdapter implements AgentAuditPort {

    private final SecurityAuditRepository securityAuditRepository;
    private final UserRepository userRepository;
    private final SecurityAuditDomainMapper mapper;

    @Override
    public SecurityAudit save(SecurityAudit securityAudit) {
        SecurityAuditEntity entity = mapper.toEntity(securityAudit);
        UserEntity user = userRepository.getReferenceById(securityAudit.getUserId());
        entity.setUser(user);
        SecurityAuditEntity saved = securityAuditRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<SecurityAudit> findByAgentId(String agentId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        return securityAuditRepository
                .findByDeviceIdOrderByTimestampDesc(agentId, pageRequest)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
