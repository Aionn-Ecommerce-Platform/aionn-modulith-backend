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

    private AuthSessionCleanupScheduler scheduler;
    private static final Instant FIXED_NOW = Instant.parse("2026-07-12T10:00:00Z");

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        scheduler = new AuthSessionCleanupScheduler(authSessionPersistence, java.time.Clock.fixed(FIXED_NOW, java.time.ZoneOffset.UTC));
    }

    @Test
    void purgesSessionsIdleBeyondRetentionWindow() {
        when(authSessionPersistence.deleteIdleBefore(any())).thenReturn(3);
        Instant expectedCutoff = FIXED_NOW.minus(Duration.ofDays(90));

        scheduler.purgeIdleSessions();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.captor();
        verify(authSessionPersistence).deleteIdleBefore(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isEqualTo(expectedCutoff);
    }

    @Test
    void purgeHandlesZeroDeletedRows() {
        when(authSessionPersistence.deleteIdleBefore(any())).thenReturn(0);

        scheduler.purgeIdleSessions();

        verify(authSessionPersistence).deleteIdleBefore(any());
    }
}
