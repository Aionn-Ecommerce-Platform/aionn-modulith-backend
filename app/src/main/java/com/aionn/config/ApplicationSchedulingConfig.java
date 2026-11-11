package com.aionn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Business scheduler for {@code @Scheduled} jobs that do not name a scheduler.
 *
 * <p>The pool size is configuration, not code: it lives in {@code application.yml} under
 * {@code spring.task.scheduling.pool.size} and can be overridden per environment through
 * {@code SCHEDULER_POOL_SIZE}. There is deliberately no default here - a missing property is a
 * deployment error, and silently falling back to a number the tests do not exercise is how a pool
 * ends up smaller than the job count.
 *
 * <p>The outbox dispatcher names its own {@code outboxTaskScheduler}; {@code @Async} is not enabled,
 * so no application work shares this pool through an executor fallback.
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationSchedulingConfig {

    /**
     * A single thread serialises every job and one slow job stalls all the others, so the configured
     * value is floored at two rather than trusted blindly.
     */
    private static final int MINIMUM_POOL_SIZE = 2;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler(
            @Value("${spring.task.scheduling.pool.size}") int configuredPoolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(MINIMUM_POOL_SIZE, configuredPoolSize));
        scheduler.setThreadNamePrefix("aionn-sched-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
