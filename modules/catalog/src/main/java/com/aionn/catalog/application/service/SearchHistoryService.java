package com.aionn.catalog.application.service;

import com.aionn.catalog.application.port.out.product.UserBrowsingHistoryPersistencePort;
import com.aionn.catalog.domain.model.UserBrowsingHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchHistoryService {

    private final UserBrowsingHistoryPersistencePort browsingHistoryPersistencePort;

    @Transactional(readOnly = true)
    public List<String> getRecentSearches(String userId) {
        return browsingHistoryPersistencePort.findByUserId(userId)
                .map(UserBrowsingHistory::getRecentSearches)
                .map(List::copyOf)
                .orElseGet(List::of);
    }

    @Transactional
    public List<String> recordSearches(String userId, List<String> queries) {
        UserBrowsingHistory history = browsingHistoryPersistencePort.findByUserId(userId)
                .orElseGet(() -> UserBrowsingHistory.create(userId));
        for (int index = queries.size() - 1; index >= 0; index--) {
            history.recordSearch(queries.get(index));
        }
        UserBrowsingHistory saved = browsingHistoryPersistencePort.save(history);
        return List.copyOf(saved.getRecentSearches());
    }
}
