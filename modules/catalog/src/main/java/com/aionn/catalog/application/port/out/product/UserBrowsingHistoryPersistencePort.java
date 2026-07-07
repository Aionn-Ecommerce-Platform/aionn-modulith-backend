package com.aionn.catalog.application.port.out.product;

import com.aionn.catalog.domain.model.UserBrowsingHistory;

import java.util.Optional;

public interface UserBrowsingHistoryPersistencePort {

    UserBrowsingHistory save(UserBrowsingHistory history);

    Optional<UserBrowsingHistory> findByUserId(String userId);
}
