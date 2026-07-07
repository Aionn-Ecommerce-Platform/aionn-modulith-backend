package com.aionn.catalog.infrastructure.persistence.adapter.product;

import com.aionn.catalog.application.port.out.product.UserBrowsingHistoryPersistencePort;
import com.aionn.catalog.domain.model.UserBrowsingHistory;
import com.aionn.catalog.infrastructure.persistence.mapper.UserBrowsingHistoryDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.product.UserBrowsingHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserBrowsingHistoryPersistenceAdapter implements UserBrowsingHistoryPersistencePort {

    private final UserBrowsingHistoryRepository repository;
    private final UserBrowsingHistoryDomainMapper mapper;

    @Override
    public UserBrowsingHistory save(UserBrowsingHistory history) {
        return mapper.toDomain(repository.save(mapper.toEntity(history)));
    }

    @Override
    public Optional<UserBrowsingHistory> findByUserId(String userId) {
        return repository.findById(userId).map(mapper::toDomain);
    }
}
