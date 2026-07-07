package com.aionn.catalog.infrastructure.persistence.adapter.product;

import com.aionn.catalog.domain.model.UserBrowsingHistory;
import com.aionn.catalog.infrastructure.persistence.entity.UserBrowsingHistoryEntity;
import com.aionn.catalog.infrastructure.persistence.mapper.UserBrowsingHistoryDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.product.UserBrowsingHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBrowsingHistoryPersistenceAdapterTest {

    private static final String USER_ID = "01HZUSR0000000000000000001";

    @Mock
    private UserBrowsingHistoryRepository repository;
    @Mock
    private UserBrowsingHistoryDomainMapper mapper;

    @InjectMocks
    private UserBrowsingHistoryPersistenceAdapter adapter;

    @Test
    void saveRoundTripsThroughMapper() {
        UserBrowsingHistory domain = UserBrowsingHistory.create(USER_ID);
        UserBrowsingHistoryEntity entity = new UserBrowsingHistoryEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.save(domain)).isSameAs(domain);
    }

    @Test
    void findByUserIdReturnsMappedDomain() {
        UserBrowsingHistoryEntity entity = new UserBrowsingHistoryEntity();
        UserBrowsingHistory domain = UserBrowsingHistory.create(USER_ID);
        when(repository.findById(USER_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByUserId(USER_ID)).contains(domain);
    }

    @Test
    void findByUserIdReturnsEmptyWhenMissing() {
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findByUserId(USER_ID)).isEmpty();
    }
}
