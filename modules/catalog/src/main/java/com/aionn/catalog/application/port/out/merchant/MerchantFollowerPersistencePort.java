package com.aionn.catalog.application.port.out.merchant;

import java.time.Instant;
import java.util.List;

public interface MerchantFollowerPersistencePort {

    void follow(String merchantId, String userId, Instant followedAt);

    void unfollow(String merchantId, String userId);

    boolean exists(String merchantId, String userId);

    long countByMerchantId(String merchantId);

    List<String> findUserIdsByMerchantId(String merchantId);
}
