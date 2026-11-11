package com.aionn.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Explicit executors for work that must not run on the calling thread.
 *
 * <p>{@code @EnableAsync} is only meaningful with a well-defined executor: {@code ThreadPoolTaskScheduler}
 * also implements {@code TaskExecutor}, so without these beans the {@code @Async} fallback can silently
 * land on the scheduled-job pool and compete with the outbox dispatcher.
 *
 * <p>Every pool is bounded. Callers that must not block use the discard handlers: the work they submit
 * is durable elsewhere (notification rows stay {@code PENDING} for the retry sweep, the catalog index is
 * rebuilt on startup), so dropping the trigger is safe while dropping the request thread is not.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncExecutionConfig {

    /** Default executor for plain {@code @Async} methods. */
    public static final String DEFAULT_EXECUTOR = "taskExecutor";

    /** Provider I/O (SMTP, SMS, push). Bounded so an outage cannot exhaust the heap. */
    public static final String NOTIFICATION_EXECUTOR = "notificationTaskExecutor";

    /** Single thread: OpenSearch writes for one product must not overtake each other. */
    public static final String SEARCH_INDEX_EXECUTOR = "searchIndexTaskExecutor";

    @Bean(name = DEFAULT_EXECUTOR)
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = baseExecutor("aionn-async-", 8, 500);
        executor.setMaxPoolSize(16);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    @Bean(name = NOTIFICATION_EXECUTOR)
    public ThreadPoolTaskExecutor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = baseExecutor("aionn-notify-", 4, 5_000);
        executor.setMaxPoolSize(4);
        executor.setRejectedExecutionHandler(discardWhenSaturated(NOTIFICATION_EXECUTOR));
        return executor;
    }

    @Bean(name = SEARCH_INDEX_EXECUTOR)
    public ThreadPoolTaskExecutor searchIndexTaskExecutor() {
        ThreadPoolTaskExecutor executor = baseExecutor("aionn-search-", 1, 5_000);
        executor.setMaxPoolSize(1);
        executor.setRejectedExecutionHandler(discardWhenSaturated(SEARCH_INDEX_EXECUTOR));
        return executor;
    }

    private static ThreadPoolTaskExecutor baseExecutor(String prefix, int corePoolSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(prefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    /**
     * A saturated queue means the durable record still exists, so the trigger is dropped with a warning
     * instead of blocking the caller (which would be the outbox dispatcher thread).
     */
    private static RejectedExecutionHandler discardWhenSaturated(String executorName) {
        return (task, executor) -> log.warn(
                "{} is saturated (queue={}); dropping the async trigger, the durable record will be retried",
                executorName, executor.getQueue().size());
    }
}
