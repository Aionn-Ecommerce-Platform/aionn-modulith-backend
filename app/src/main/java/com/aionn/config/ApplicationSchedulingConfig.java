package com.aionn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Owns the scheduler that every {@code @Scheduled} business job runs on.
 *
 * <p>This bean has to exist explicitly. Spring Boot's {@code TaskSchedulingAutoConfiguration} is
 * {@code @ConditionalOnMissingBean(TaskScheduler.class)}, and the outbox already registers one, so
 * Boot never creates a {@code taskScheduler} of its own. With only
 * {@code outboxTaskScheduler} in the context - a pool of one - the
 * {@code ScheduledAnnotationBeanPostProcessor} resolves it as the default for every job that does not
 * name a scheduler, and the whole application's background work serialises onto the outbox thread. A
 * slow offline rebuild then stalls the outbox dispatcher, which stops all integration-event delivery.
 *
 * <p>The bean is named {@code taskScheduler} on purpose: that is the name Spring falls back to when
 * several {@code TaskScheduler} beans are present, so the outbox keeps its dedicated single thread
 * (it coordinates with row locks, leases and {@code FOR UPDATE SKIP LOCKED}) while business jobs get
 * a pool sized for concurrency.
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationSchedulingConfig {

    /**
     * At least one thread per scheduled job, so no job can ever queue behind another. Every job either
     * holds a ShedLock or finishes in well under its interval, so the threads are idle almost all the
     * time and the pool costs nothing but the reservation.
     *
     * <p>{@code ApplicationSchedulingConfigTest} counts the {@code @Scheduled} methods on the classpath
     * and fails if this falls below them, which is what keeps the number honest as jobs are added. It
     * counts from bytecode rather than by hand because a job written with a fully qualified annotation -
     * which {@code InMemoryRegistrationRateLimiter} does - is easy to miss in a grep.
     */
    static final int DEFAULT_POOL_SIZE = 20;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler(
            @Value("${spring.task.scheduling.pool.size:" + DEFAULT_POOL_SIZE + "}") int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(2, poolSize));
        scheduler.setThreadNamePrefix("aionn-sched-");
        // Let a running job finish rather than abandoning it mid-transaction; bounded by the
        // lifecycle timeout so shutdown still completes.
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
