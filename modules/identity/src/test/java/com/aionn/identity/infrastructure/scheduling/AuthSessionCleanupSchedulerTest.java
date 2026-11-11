package com.aionn.identity.infrastructure.scheduling;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;

import com.aionn.identity.application.port.out.auth.AuthSessionPersistencePort;
import com.aionn.identity.infrastructure.config.properties.AuthSessionProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @BeforeEach
    void setUp() {
        scheduler = new AuthSessionCleanupScheduler(
                authSessionPersistence,
                new AuthSessionProperties(30L, 90L),
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
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
    void retentionWindowComesFromConfiguration() {
        AuthSessionCleanupScheduler configured = new AuthSessionCleanupScheduler(
                authSessionPersistence,
                new AuthSessionProperties(30L, 7L),
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        when(authSessionPersistence.deleteIdleBefore(any())).thenReturn(1);

        configured.purgeIdleSessions();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.captor();
        verify(authSessionPersistence).deleteIdleBefore(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isEqualTo(FIXED_NOW.minus(Duration.ofDays(7)));
    }

    @Test
    void purgeHandlesZeroDeletedRows() {
        when(authSessionPersistence.deleteIdleBefore(any())).thenReturn(0);

        scheduler.purgeIdleSessions();

        verify(authSessionPersistence).deleteIdleBefore(any());
    }
}
