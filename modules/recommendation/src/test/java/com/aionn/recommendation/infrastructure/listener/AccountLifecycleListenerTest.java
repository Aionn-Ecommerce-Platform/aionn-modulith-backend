package com.aionn.recommendation.infrastructure.listener;

import com.aionn.recommendation.application.service.InteractionRetentionService;
import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import com.aionn.sharedkernel.integration.event.identity.AccountDeletedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.EventListener;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AccountLifecycleListenerTest {

    @Mock
    private InteractionRetentionService retentionService;

    private AccountLifecycleListener listener() {
        return new AccountLifecycleListener(retentionService);
    }

    @Test
    void aDeletedAccountLosesItsBehaviouralData() {
        listener().onAccountDeleted(new AccountDeletedIntegrationEvent(
                "evt-1", "user-1", Instant.parse("2026-09-05T12:00:00Z")));

        verify(retentionService).eraseUser("user-1");
    }

    @Test
    void aFailedErasureIsRethrownSoTheOutboxRetriesIt() {
        // Silently swallowing this would leave a deleted account's browsing history in
        // place with
        // nothing to signal it, which is the one failure mode this listener exists to
        // prevent.
        doThrow(new IllegalStateException("db down")).when(retentionService).eraseUser("user-1");
        AccountLifecycleListener listener = listener();
        AccountDeletedIntegrationEvent event = new AccountDeletedIntegrationEvent(
                "evt-1", "user-1", Instant.parse("2026-09-05T12:00:00Z"));
        assertThatThrownBy(() -> listener.onAccountDeleted(event))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void theHandlerIsDeclaredAgainstAnIntegrationEvent() {
        // Required for the inbox aspect to read the event ID; without it a redelivered
        // deletion would
        // re-run the erasure, which is harmless here but the same declaration rule
        // guards the ingest
        // listener where redelivery would double-count a purchase.
        List<Method> handlers = Arrays.stream(AccountLifecycleListener.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(EventListener.class))
                .toList();

        assertThat(handlers).hasSize(1);
        assertThat(IntegrationEvent.class).isAssignableFrom(handlers.getFirst().getParameterTypes()[0]);
    }
}
