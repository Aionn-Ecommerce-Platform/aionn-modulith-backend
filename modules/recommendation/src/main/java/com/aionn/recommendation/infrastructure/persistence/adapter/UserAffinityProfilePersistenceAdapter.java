package com.aionn.recommendation.infrastructure.persistence.adapter;

import com.aionn.recommendation.application.port.out.UserAffinityProfilePersistencePort;
import com.aionn.recommendation.domain.model.UserAffinityProfile;
import com.aionn.recommendation.infrastructure.persistence.mapper.UserAffinityProfileDomainMapper;
import com.aionn.recommendation.infrastructure.persistence.repository.UserAffinityProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserAffinityProfilePersistenceAdapter implements UserAffinityProfilePersistencePort {

    private final UserAffinityProfileRepository jpa;
    private final UserAffinityProfileDomainMapper mapper;

    @Override
    public Optional<UserAffinityProfile> findByUserId(String userId) {
        return jpa.findById(userId).map(mapper::toDomain);
    }

    @Override
    public void save(UserAffinityProfile profile, Instant refreshedAt) {
        jpa.save(mapper.toEntity(profile, refreshedAt));
    }

    @Override
    public int deleteByUserId(String userId) {
        return jpa.deleteByUserId(userId);
    }
}
