package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.UserBrowsingHistory;
import com.aionn.catalog.infrastructure.persistence.entity.UserBrowsingHistoryEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserBrowsingHistoryDomainMapperTest {

    private final UserBrowsingHistoryDomainMapper mapper = new UserBrowsingHistoryDomainMapper();

    @Test
    void mapsRecentSearchesBetweenDomainAndEntity() {
        UserBrowsingHistory domain = new UserBrowsingHistory(
                "user-1", new ArrayList<>(List.of("cat-1")), new ArrayList<>(List.of("brand-1")),
                new ArrayList<>(List.of("laptop", "phone")));

        UserBrowsingHistoryEntity entity = mapper.toEntity(domain);
        UserBrowsingHistory mapped = mapper.toDomain(entity);

        assertThat(entity.getRecentSearches()).containsExactly("laptop", "phone");
        assertThat(mapped.getRecentSearches()).containsExactly("laptop", "phone");
    }

    @Test
    void mapsNullInBothDirections() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
