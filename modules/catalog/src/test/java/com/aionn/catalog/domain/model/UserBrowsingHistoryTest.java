package com.aionn.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserBrowsingHistoryTest {

    @Test
    void createStartsEmpty() {
        UserBrowsingHistory history = UserBrowsingHistory.create("user-1");

        assertThat(history.getUserId()).isEqualTo("user-1");
        assertThat(history.getCategoryIds()).isEmpty();
        assertThat(history.getBrandIds()).isEmpty();
        assertThat(history.getRecentSearches()).isEmpty();
    }

    @Test
    void recordSearchNormalizesDeduplicatesAndMovesQueryToFront() {
        UserBrowsingHistory history = UserBrowsingHistory.create("user-1");

        history.recordSearch("  wireless   headphones ");
        history.recordSearch("phone");
        history.recordSearch("WIRELESS HEADPHONES");

        assertThat(history.getRecentSearches()).containsExactly("WIRELESS HEADPHONES", "phone");
    }

    @Test
    void recordSearchKeepsOnlyFiveMostRecentQueries() {
        UserBrowsingHistory history = UserBrowsingHistory.create("user-1");

        for (int index = 0; index < 8; index++) {
            history.recordSearch("query-" + index);
        }

        assertThat(history.getRecentSearches())
                .containsExactly("query-7", "query-6", "query-5", "query-4", "query-3");
    }

    @Test
    void recordSearchIgnoresNullAndBlankQueries() {
        UserBrowsingHistory history = UserBrowsingHistory.create("user-1");

        history.recordSearch(null);
        history.recordSearch("   ");

        assertThat(history.getRecentSearches()).isEmpty();
    }

    @Test
    void trackViewMovesMostRecentToFront() {
        UserBrowsingHistory history = UserBrowsingHistory.create("user-1");

        history.trackView(List.of("cat-a"), "brand-a");
        history.trackView(List.of("cat-b"), "brand-b");
        history.trackView(List.of("cat-a"), "brand-a");

        assertThat(history.getCategoryIds()).containsExactly("cat-a", "cat-b");
        assertThat(history.getBrandIds()).containsExactly("brand-a", "brand-b");
    }

    @Test
    void trackViewCapsPreferencesAtFive() {
        UserBrowsingHistory history = UserBrowsingHistory.create("user-1");

        for (int i = 0; i < 8; i++) {
            history.trackView(List.of("cat-" + i), "brand-" + i);
        }

        assertThat(history.getCategoryIds()).hasSize(5);
        assertThat(history.getBrandIds()).hasSize(5);
        assertThat(history.getCategoryIds().get(0)).isEqualTo("cat-7");
    }

    @Test
    void trackViewToleratesNullInputs() {
        UserBrowsingHistory history = UserBrowsingHistory.create("user-1");

        history.trackView(null, null);

        assertThat(history.getCategoryIds()).isEmpty();
        assertThat(history.getBrandIds()).isEmpty();
    }
}
