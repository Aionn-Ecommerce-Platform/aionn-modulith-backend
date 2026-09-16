package com.aionn.notification.infrastructure.persistence.adapter.notification;

import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.infrastructure.persistence.entity.NotificationEntity;
import com.aionn.notification.infrastructure.persistence.mapper.NotificationDomainMapper;
import com.aionn.notification.infrastructure.persistence.repository.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NotificationPersistenceAdapterTest {

    @Test
    void flushesNewNotificationBeforeReturningToJdbcDeliveryAttemptWriter() {
        assertSaveFlushes(null);
    }

    @Test
    void flushesUpdatedNotificationUsingExistingEntity() {
        assertSaveFlushes(mock(NotificationEntity.class));
    }

    private void assertSaveFlushes(NotificationEntity existing) {
        NotificationRepository repository = mock(NotificationRepository.class);
        NotificationDomainMapper mapper = mock(NotificationDomainMapper.class);
        Notification notification = mock(Notification.class);
        NotificationEntity entity = mock(NotificationEntity.class);
        NotificationEntity persisted = mock(NotificationEntity.class);
        Notification saved = mock(Notification.class);
        when(notification.getNotiId()).thenReturn("notification");
        when(repository.findById("notification")).thenReturn(Optional.ofNullable(existing));
        when(mapper.toEntity(notification, existing)).thenReturn(entity);
        when(repository.saveAndFlush(entity)).thenReturn(persisted);
        when(mapper.toDomain(persisted)).thenReturn(saved);

        NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(repository, mapper);
        assertThat(adapter.save(notification)).isSameAs(saved);

        var order = inOrder(repository, mapper);
        order.verify(repository).findById("notification");
        order.verify(mapper).toEntity(notification, existing);
        order.verify(repository).saveAndFlush(entity);
        order.verify(mapper).toDomain(persisted);
        verify(repository, never()).save(any());
    }
}
