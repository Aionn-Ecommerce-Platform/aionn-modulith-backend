package com.aionn.chat.infrastructure.persistence.adapter.block;

import com.aionn.chat.application.port.out.UserBlockPersistencePort;
import com.aionn.chat.domain.model.UserBlock;
import com.aionn.chat.infrastructure.persistence.entity.UserBlockEntity;
import com.aionn.chat.infrastructure.persistence.mapper.UserBlockDomainMapper;
import com.aionn.chat.infrastructure.persistence.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserBlockPersistenceAdapter implements UserBlockPersistencePort {

    private final UserBlockRepository jpa;
    private final UserBlockDomainMapper mapper;

    @Override
    public UserBlock save(UserBlock block) {
        UserBlockEntity existing = jpa.findById(block.getBlockId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(block, existing)));
    }

    @Override
    public Optional<UserBlock> findActive(String blockerId, String blockedId) {
        return jpa.findByBlockerIdAndBlockedIdAndActiveTrue(blockerId, blockedId).map(mapper::toDomain);
    }

    @Override
    public boolean exists(String blockerId, String blockedId) {
        return jpa.existsByBlockerIdAndBlockedIdAndActiveTrue(blockerId, blockedId);
    }

    @Override
    public List<UserBlock> findByBlocker(String blockerId) {
        return jpa.findByBlockerIdOrderByCreatedAtDesc(blockerId).stream().map(mapper::toDomain).toList();
    }
}

