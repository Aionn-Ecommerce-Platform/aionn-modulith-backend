package com.aionn.notification.application.port.out;

import com.aionn.notification.domain.model.DeviceToken;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenPersistencePort {

    DeviceToken save(DeviceToken token);

    Optional<DeviceToken> findById(String tokenId);

    Optional<DeviceToken> findByUserAndToken(String userId, String deviceToken);

    List<DeviceToken> findActiveByUserId(String userId);
}

