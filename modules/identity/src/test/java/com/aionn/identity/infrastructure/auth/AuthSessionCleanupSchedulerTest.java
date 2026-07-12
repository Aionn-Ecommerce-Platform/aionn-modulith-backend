package com.aionn.identity.infrastructure.auth;

import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionCleanupSchedulerTest {

    @Mock
    private AuthSessionPersistencePort authSessionPersistence;

    @InjectMocks
    private AuthSessionCleanupScheduler scheduler;

    @Test
    void purgesSessionsIdleBeyondRetentionWindow() {
        when(authSessionPersistence.deleteIdleBefore(any())).thenReturn(3);
        Instant before = Instant.now().minus(Duration.ofDays(90));

        scheduler.purgeIdleSessions();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.captor();
        verify(authSessionPersistence).deleteIdleBefore(cutoffCaptor.capture());
        Instant after = Instant.now().minus(Duration.ofDays(90));
        assertThat(cutoffCaptor.getValue()).isBetween(before.minus(Duration.ofSeconds(5)),
                after.plus(Duration.ofSeconds(5)));
    }

    @Test
    void purgeHandlesZeroDeletedRows() {
        when(authSessionPersistence.deleteIdleBefore(any())).thenReturn(0);

        scheduler.purgeIdleSessions();

        verify(authSessionPersistence).deleteIdleBefore(any());
    }
}
