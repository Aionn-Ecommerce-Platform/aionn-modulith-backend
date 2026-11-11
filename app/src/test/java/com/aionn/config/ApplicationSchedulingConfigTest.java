package com.aionn.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the scheduler topology.
 *
 * <p>Every business job that does not name a scheduler resolves to the bean called {@code taskScheduler}.
 * If that bean is absent or single-threaded, all background work in the application serialises onto one
 * thread and a slow job stalls the outbox dispatcher, which stops integration-event delivery entirely -
 * including order, payment and shipping events that have nothing to do with the job that blocked them.
 *
 * <p>The assertions read the shipped configuration rather than a constant, so the number tested is the
 * number production runs. A YAML edit that shrinks the pool below the job count fails here.
 */
class ApplicationSchedulingConfigTest {

    private static final String POOL_SIZE_PROPERTY = "spring.task.scheduling.pool.size";

    private final ApplicationSchedulingConfig config = new ApplicationSchedulingConfig();

    @Test
    void theConfiguredPoolHasAThreadForEveryJobThatWouldOtherwiseShareOne() throws IOException {
        // This is the invariant that makes the topology safe, and it is the one that silently rots: adding
        // a seventeenth job to a sixteen-thread pool does not fail anything at build time, it just means
        // two jobs can now queue behind each other.
        List<String> jobs = unqualifiedScheduledJobs();

        assertThat(jobs)
                .as("scheduled jobs resolving to the default scheduler")
                .isNotEmpty();
        assertThat(configuredPoolSize())
                .as("spring.task.scheduling.pool.size must cover every job found: %s", jobs)
                .isGreaterThanOrEqualTo(jobs.size());
    }

    @Test
    void theOutboxKeepsItsOwnSchedulerAndIsNotCounted() throws IOException {
        // The dispatcher polls every second and coordinates through row locks, so it is deliberately
        // isolated from business jobs rather than sharing the pool. It names its scheduler explicitly,
        // which is what keeps it out of the count above.
        List<String> dedicated = scheduledJobsNaming("outboxTaskScheduler");

        assertThat(dedicated).isNotEmpty();
    }

    @Test
    void theBeanIsNamedSoSpringResolvesItWhenSeveralSchedulersExist() {
        // ScheduledAnnotationBeanPostProcessor asks for a unique TaskScheduler first; with the outbox one
        // also present that lookup is ambiguous, and it then falls back to exactly this bean name. The
        // registration below is that fallback path, exercised rather than asserted from reflection.
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources()
                    .addFirst(new MapPropertySource("scheduling-test", Map.of(POOL_SIZE_PROPERTY, "24")));
            context.register(OutboxSchedulerStub.class, ApplicationSchedulingConfig.class);
            context.refresh();

            // Identified by thread-name prefix rather than by getPoolSize(): once Spring has initialised
            // the bean, getPoolSize() reports the live executor's thread count, which is zero until a
            // task has actually run. The configured size is asserted behaviourally below.
            assertThat(context.getBean("taskScheduler", ThreadPoolTaskScheduler.class)
                    .getThreadNamePrefix()).isEqualTo("aionn-sched-");
            assertThat(context.getBean("outboxTaskScheduler", ThreadPoolTaskScheduler.class)
                    .getThreadNamePrefix()).isEqualTo("outbox-dispatch-");
            assertThat(context.getBeanNamesForType(ThreadPoolTaskScheduler.class))
                    .contains("taskScheduler", "outboxTaskScheduler");
            assertThat(context.getBeanProvider(ThreadPoolTaskScheduler.class).getIfUnique())
                    .as("ambiguous by type, so the name fallback is what selects the business pool")
                    .isNull();
        }
    }

    @Test
    void businessJobsActuallyRunConcurrently() throws Exception {
        // Uses the shipped configuration rather than a small stand-in, so this is the pool production
        // gets being shown to deliver the concurrency the topology depends on.
        int poolSize = configuredPoolSize();
        ThreadPoolTaskScheduler scheduler = config.taskScheduler(poolSize);
        try {
            scheduler.initialize();
            List<Future<?>> futures = new ArrayList<>();
            CountDownLatch allRunning = new CountDownLatch(poolSize);
            CountDownLatch release = new CountDownLatch(1);
            for (int index = 0; index < poolSize; index++) {
                futures.add(scheduler.submit(() -> {
                    allRunning.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }

            // A pool of one could never satisfy this: the first task would hold the only thread until
            // released, which is precisely how a slow rebuild used to stall the outbox dispatcher.
            assertThat(allRunning.await(10, TimeUnit.SECONDS))
                    .as("every job must get a thread without waiting for another to finish")
                    .isTrue();

            release.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    void thePoolSizeIsOverridableButNeverBelowTwo() {
        ThreadPoolTaskScheduler zero = config.taskScheduler(0);
        ThreadPoolTaskScheduler configured = config.taskScheduler(24);
        try {
            // A misconfigured pool must degrade to something usable rather than to a scheduler that
            // throws on the first job, which would take every background job down at once.
            assertThat(zero.getPoolSize()).isEqualTo(2);
            assertThat(configured.getPoolSize()).isEqualTo(24);
        } finally {
            zero.shutdown();
            configured.shutdown();
        }
    }

    /**
     * Reads the value the application actually starts with: the YAML entry, with its
     * {@code ${SCHEDULER_POOL_SIZE:...}} placeholder resolved the same way Spring resolves it.
     */
    private static int configuredPoolSize() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        ConfigurableEnvironment environment = new StandardEnvironment();
        sources.forEach(source -> environment.getPropertySources().addLast(source));
        String raw = environment.getProperty(POOL_SIZE_PROPERTY);
        if (raw == null) {
            throw new AssertionError(POOL_SIZE_PROPERTY + " is not configured in application.yml");
        }
        return Integer.parseInt(environment.resolvePlaceholders(raw).trim());
    }

    /**
     * Reads {@code @Scheduled} methods off the classpath bytecode rather than loading the classes, so
     * counting jobs cannot trigger a static initialiser or need the whole application context.
     */
    private static List<String> unqualifiedScheduledJobs() throws IOException {
        return scheduledJobs("");
    }

    private static List<String> scheduledJobsNaming(String schedulerBeanName) throws IOException {
        return scheduledJobs(schedulerBeanName);
    }

    private static List<String> scheduledJobs(String schedulerBeanName) throws IOException {
        MetadataReaderFactory readers = new SimpleMetadataReaderFactory();
        Resource[] classes = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:com/aionn/**/*.class");

        List<String> jobs = new ArrayList<>();
        for (Resource resource : classes) {
            if (!resource.isReadable()) {
                continue;
            }
            for (MethodMetadata method : readers.getMetadataReader(resource)
                    .getAnnotationMetadata()
                    .getAnnotatedMethods(Scheduled.class.getName())) {
                Object scheduler = method
                        .getAnnotationAttributes(Scheduled.class.getName())
                        .get("scheduler");
                if (schedulerBeanName.equals(scheduler)) {
                    jobs.add(method.getDeclaringClassName() + "#" + method.getMethodName());
                }
            }
        }
        return jobs;
    }

    /**
     * Stands in for {@code OutboxSchedulingConfiguration}, which the real application supplies. Only the
     * bean name and the pool of one matter: that is what makes the {@code TaskScheduler} lookup ambiguous
     * and forces Spring onto the name fallback.
     */
    @Configuration(proxyBeanMethods = false)
    static class OutboxSchedulerStub {

        @Bean
        ThreadPoolTaskScheduler outboxTaskScheduler() {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.setThreadNamePrefix("outbox-dispatch-");
            return scheduler;
        }
    }
}
