package com.aionn.identity.infrastructure.scheduling;

import com.aionn.identity.application.port.out.auth.RefreshTokenStorePort;
import com.aionn.identity.application.port.out.integration.IdentityIntegrationEventPublisherPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionSchedulerTest {

    @Mock private AccountDeletionProcessor processor;
    @Mock private RefreshTokenStorePort refreshTokenStore;
    @Mock private IdentityIntegrationEventPublisherPort integrationEventPublisher;

    @Test
    void removesRefreshTokensAfterDatabaseCompletion() {
        when(processor.completeDueRequests()).thenReturn(new AccountDeletionProcessor.Result(
                1, List.of("session-1", "session-2"), List.of("user-1")));
        AccountDeletionScheduler scheduler = scheduler();

        scheduler.completeDueRequests();

        verify(refreshTokenStore).revokeBySessionId("session-1");
        verify(refreshTokenStore).revokeBySessionId("session-2");
    }

    @Test
    void redisFailureCannotRollBackCompletedDatabaseDeletion() {
        when(processor.completeDueRequests()).thenReturn(
                new AccountDeletionProcessor.Result(1, List.of("session-1"), List.of("user-1")));
        doThrow(new IllegalStateException("redis unavailable"))
                .when(refreshTokenStore).revokeBySessionId("session-1");
        AccountDeletionScheduler scheduler = scheduler();

        assertThatCode(scheduler::completeDueRequests).doesNotThrowAnyException();
    }

    @Test
    void announcesEachCompletedDeletionSoConsumersCanEraseDerivedData() {
        when(processor.completeDueRequests()).thenReturn(new AccountDeletionProcessor.Result(
                2, List.of("session-1"), List.of("user-1", "user-2")));
        AccountDeletionScheduler scheduler = scheduler();

        scheduler.completeDueRequests();

        verify(integrationEventPublisher).publishAccountDeleted("user-1");
        verify(integrationEventPublisher).publishAccountDeleted("user-2");
    }

    @Test
    void publishFailureCannotRollBackCompletedDatabaseDeletion() {
        when(processor.completeDueRequests()).thenReturn(
                new AccountDeletionProcessor.Result(1, List.of(), List.of("user-1")));
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(integrationEventPublisher).publishAccountDeleted("user-1");
        AccountDeletionScheduler scheduler = scheduler();

        assertThatCode(scheduler::completeDueRequests).doesNotThrowAnyException();
    }

    private AccountDeletionScheduler scheduler() {
        return new AccountDeletionScheduler(processor, refreshTokenStore, integrationEventPublisher);
    }
}
