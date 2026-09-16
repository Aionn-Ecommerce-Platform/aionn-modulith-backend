package com.aionn.sharedkernel.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.ClockProvider;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.support.KeepAliveLockProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class DistributedSchedulerLockConfigurationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration LEASE = Duration.ofMinutes(30);

    @Test
    void productionProviderAutomaticallyExtendsLeaseAndStopsOnUnlock() {
        var configuration = new DistributedSchedulerLockConfiguration();
        DataSource dataSource = mock(DataSource.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> renewal = mock(ScheduledFuture.class);
        SimpleLock acquired = mock(SimpleLock.class);
        SimpleLock extended = mock(SimpleLock.class);
        SimpleLock extendedAgain = mock(SimpleLock.class);
        var lockConfiguration = new LockConfiguration(NOW, "recommendation-rebuild", LEASE, Duration.ZERO);
        doReturn(renewal).when(executor).scheduleAtFixedRate(
                any(Runnable.class), anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS));
        when(acquired.extend(LEASE, Duration.ZERO)).thenReturn(Optional.of(extended));
        when(extended.extend(LEASE, Duration.ZERO)).thenReturn(Optional.of(extendedAgain));

        // Intercept only the database boundary; the production factory must create the real keep-alive wrapper.
        try (var clock = mockStatic(ClockProvider.class);
                var jdbc = mockConstruction(JdbcTemplateLockProvider.class, (provider, construction) -> {
                    var jdbcConfiguration = (JdbcTemplateLockProvider.Configuration) construction.arguments().getFirst();
                    assertThat(jdbcConfiguration.getJdbcTemplate().getDataSource()).isSameAs(dataSource);
                    assertThat(jdbcConfiguration.getUseDbTime()).isTrue();
                    when(provider.lock(lockConfiguration)).thenReturn(Optional.of(acquired));
                })) {
            clock.when(ClockProvider::now).thenReturn(NOW);
            LockProvider provider = configuration.schedulerLockProvider(dataSource, executor);
            assertThat(provider).isInstanceOf(KeepAliveLockProvider.class);
            SimpleLock lock = provider.lock(lockConfiguration).orElseThrow();
            assertThat(jdbc.constructed()).hasSize(1);
            verify(jdbc.constructed().getFirst()).lock(lockConfiguration);

            var callback = ArgumentCaptor.forClass(Runnable.class);
            long halfLease = LEASE.dividedBy(2).toMillis();
            verify(executor).scheduleAtFixedRate(callback.capture(), eq(halfLease), eq(halfLease),
                    eq(TimeUnit.MILLISECONDS));
            verifyNoInteractions(acquired);

            // Drive the captured periodic task directly: no real scheduler, clock advance, or sleeps are needed.
            callback.getValue().run();
            verify(acquired).extend(LEASE, Duration.ZERO);
            callback.getValue().run();
            verify(extended).extend(LEASE, Duration.ZERO);

            lock.unlock();
            verify(renewal).cancel(false);
            verify(extendedAgain).unlock();
            verify(acquired, never()).unlock();
            verify(extended, never()).unlock();

            // A callback already queued when unlock cancels the future must not renew the released lease.
            callback.getValue().run();
            verifyNoMoreInteractions(acquired, extended, extendedAgain);
        }
    }

    @Test
    void productionProviderDoesNotScheduleRenewalWhenAnotherNodeOwnsTheLock() {
        var configuration = new DistributedSchedulerLockConfiguration();
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        var lockConfiguration = new LockConfiguration(NOW, "recommendation-rebuild", LEASE, Duration.ZERO);

        try (var jdbc = mockConstruction(JdbcTemplateLockProvider.class, (provider, construction) ->
                when(provider.lock(lockConfiguration)).thenReturn(Optional.empty()))) {
            LockProvider provider = configuration.schedulerLockProvider(mock(DataSource.class), executor);

            assertThat(provider.lock(lockConfiguration)).isEmpty();
            verify(jdbc.constructed().getFirst()).lock(lockConfiguration);
            verifyNoInteractions(executor);
        }
    }

    @Test
    void productionProviderStopsRenewalWhenLeaseExtensionFails() {
        var configuration = new DistributedSchedulerLockConfiguration();
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        ScheduledFuture<?> renewal = mock(ScheduledFuture.class);
        SimpleLock acquired = mock(SimpleLock.class);
        var lockConfiguration = new LockConfiguration(NOW, "recommendation-rebuild", LEASE, Duration.ZERO);
        doReturn(renewal).when(executor).scheduleAtFixedRate(
                any(Runnable.class), anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS));
        when(acquired.extend(LEASE, Duration.ZERO)).thenReturn(Optional.empty());

        try (var clock = mockStatic(ClockProvider.class);
                var jdbc = mockConstruction(JdbcTemplateLockProvider.class, (provider, construction) ->
                        when(provider.lock(lockConfiguration)).thenReturn(Optional.of(acquired)))) {
            clock.when(ClockProvider::now).thenReturn(NOW);
            LockProvider provider = configuration.schedulerLockProvider(mock(DataSource.class), executor);
            assertThat(provider.lock(lockConfiguration)).isPresent();
            verify(jdbc.constructed().getFirst()).lock(lockConfiguration);
            var callback = ArgumentCaptor.forClass(Runnable.class);
            verify(executor).scheduleAtFixedRate(callback.capture(), anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS));

            callback.getValue().run();
            verify(acquired).extend(LEASE, Duration.ZERO);
            verify(renewal).cancel(false);

            callback.getValue().run();
            verifyNoMoreInteractions(acquired, renewal);
        }
    }

    @Test
    void springContextOwnsAndShutsDownTheDedicatedExtensionExecutor() {
        ScheduledExecutorService executor;
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DataSource.class, () -> mock(DataSource.class));
            context.register(DistributedSchedulerLockConfiguration.class);
            context.refresh();

            assertThat(context.getBean(LockProvider.class)).isInstanceOf(KeepAliveLockProvider.class);
            executor = context.getBean("schedulerLockExtensionExecutor", ScheduledExecutorService.class);
            assertThat(executor.isShutdown()).isFalse();
        }
        assertThat(executor.isShutdown()).isTrue();
    }
}
