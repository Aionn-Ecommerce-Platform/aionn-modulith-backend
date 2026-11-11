package com.aionn.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The executors are what keeps {@code @Async} work off the scheduled-job pool. These assertions are
 * intentionally about the wiring: a single-threaded search worker preserves per-product ordering, and
 * every queue is bounded so a provider outage cannot grow the heap without limit.
 */
class AsyncExecutionConfigTest {

    private final AsyncExecutionConfig config = new AsyncExecutionConfig();

    @Test
    void notificationDeliveryRunsOnItsOwnBoundedPool() {
        ThreadPoolTaskExecutor executor = config.notificationTaskExecutor();

        assertThat(executor.getThreadNamePrefix()).isEqualTo("aionn-notify-");
        assertThat(executor.getCorePoolSize()).isEqualTo(4);
        assertThat(executor.getMaxPoolSize()).isEqualTo(4);
        assertThat(executor.getQueueCapacity()).isEqualTo(5_000);
    }

    @Test
    void searchIndexingIsSingleThreadedToPreserveOrder() {
        ThreadPoolTaskExecutor executor = config.searchIndexTaskExecutor();

        assertThat(executor.getThreadNamePrefix()).isEqualTo("aionn-search-");
        assertThat(executor.getCorePoolSize()).isEqualTo(1);
        assertThat(executor.getMaxPoolSize()).isEqualTo(1);
    }

    @Test
    void defaultExecutorIsSeparateFromTheNamedOnes() {
        ThreadPoolTaskExecutor executor = config.taskExecutor();

        assertThat(executor.getThreadNamePrefix()).isEqualTo("aionn-async-");
        assertThat(executor.getCorePoolSize()).isEqualTo(8);
        assertThat(executor.getMaxPoolSize()).isEqualTo(16);
    }
}
