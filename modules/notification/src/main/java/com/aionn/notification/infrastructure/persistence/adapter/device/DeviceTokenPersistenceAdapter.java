package com.aionn.notification.infrastructure.persistence.adapter.device;

import com.aionn.notification.application.port.out.DeviceTokenPersistencePort;
import com.aionn.notification.domain.model.DeviceToken;
import com.aionn.notification.infrastructure.persistence.entity.DeviceTokenEntity;
import com.aionn.notification.infrastructure.persistence.mapper.DeviceTokenDomainMapper;
import com.aionn.notification.infrastructure.persistence.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceTokenPersistenceAdapter implements DeviceTokenPersistencePort {

    private final DeviceTokenRepository jpa;
    private final DeviceTokenDomainMapper mapper;

    @Override
    public DeviceToken save(DeviceToken token) {
        DeviceTokenEntity existing = jpa.findById(token.getTokenId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(token, existing)));
    }

    @Override
    public Optional<DeviceToken> findById(String tokenId) {
        return jpa.findById(tokenId).map(mapper::toDomain);
    }

    @Override
    public Optional<DeviceToken> findByUserAndToken(String userId, String deviceToken) {
        return jpa.findByUserIdAndDeviceToken(userId, deviceToken).map(mapper::toDomain);
    }

    @Override
    public List<DeviceToken> findActiveByUserId(String userId) {
        return jpa.findByUserIdAndActiveTrue(userId).stream().map(mapper::toDomain).toList();
    }
}

