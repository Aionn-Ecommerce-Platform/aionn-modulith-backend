package com.aionn.catalog.application.service;

import com.aionn.catalog.application.port.out.product.UserBrowsingHistoryPersistencePort;
import com.aionn.catalog.domain.model.UserBrowsingHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {

    private static final String USER_ID = "user-1";

    @Mock
    private UserBrowsingHistoryPersistencePort persistencePort;

    private SearchHistoryService service;

    @BeforeEach
    void setUp() {
        service = new SearchHistoryService(persistencePort);
    }

    @Test
    void getRecentSearchesReturnsStoredQueries() {
        UserBrowsingHistory history = historyWithSearches("laptop", "keyboard");
        when(persistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(history));

        assertThat(service.getRecentSearches(USER_ID)).containsExactly("laptop", "keyboard");

        verify(persistencePort, never()).save(any());
    }

    @Test
    void getRecentSearchesReturnsEmptyListWhenHistoryDoesNotExist() {
        when(persistencePort.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(service.getRecentSearches(USER_ID)).isEmpty();
    }

    @Test
    void recordSearchesCreatesHistoryAndPreservesNewestFirstRequestOrder() {
        when(persistencePort.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(persistencePort.save(any(UserBrowsingHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<String> result = service.recordSearches(USER_ID, List.of("newest", "older"));

        assertThat(result).containsExactly("newest", "older");
    }

    @Test
    void recordSearchesMergesDuplicatesAndCapsResultAtFive() {
        UserBrowsingHistory history = historyWithSearches("phone", "laptop", "mouse", "monitor", "camera");
        when(persistencePort.findByUserId(USER_ID)).thenReturn(Optional.of(history));
        when(persistencePort.save(history)).thenReturn(history);

        List<String> result = service.recordSearches(USER_ID, List.of("PHONE", "tablet"));

        assertThat(result).containsExactly("PHONE", "tablet", "laptop", "mouse", "monitor");
        verify(persistencePort).save(history);
    }

    private static UserBrowsingHistory historyWithSearches(String... queries) {
        return new UserBrowsingHistory(USER_ID, new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(List.of(queries)));
    }
}
