package com.aionn.catalog.infrastructure.persistence.mapper;

import com.aionn.catalog.domain.model.UserBrowsingHistory;
import com.aionn.catalog.infrastructure.persistence.entity.UserBrowsingHistoryEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserBrowsingHistoryDomainMapperTest {

    private final UserBrowsingHistoryDomainMapper mapper = new UserBrowsingHistoryDomainMapper();

    @Test
    void toDomainAndBackPreservesData() {
        UserBrowsingHistoryEntity entity = UserBrowsingHistoryEntity.builder()
                .userId("user-1")
                .categoryIds(List.of("cat-a", "cat-b"))
                .brandIds(List.of("brand-a"))
                .build();

        UserBrowsingHistory domain = mapper.toDomain(entity);
        assertThat(domain.getUserId()).isEqualTo("user-1");
        assertThat(domain.getCategoryIds()).containsExactly("cat-a", "cat-b");
        assertThat(domain.getBrandIds()).containsExactly("brand-a");

        UserBrowsingHistoryEntity roundTrip = mapper.toEntity(domain);
        assertThat(roundTrip.getUserId()).isEqualTo("user-1");
        assertThat(roundTrip.getCategoryIds()).containsExactly("cat-a", "cat-b");
        assertThat(roundTrip.getBrandIds()).containsExactly("brand-a");
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }
}
