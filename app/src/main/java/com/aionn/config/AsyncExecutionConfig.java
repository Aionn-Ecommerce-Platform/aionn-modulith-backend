package com.aionn.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncExecutionConfig {

    public static final String DEFAULT_EXECUTOR = "taskExecutor";
    public static final String NOTIFICATION_EXECUTOR = "notificationTaskExecutor";
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

    private static RejectedExecutionHandler discardWhenSaturated(String executorName) {
        return (task, executor) -> log.warn(
                "{} is saturated (queue={}); dropping the async trigger, the durable record will be retried",
                executorName, executor.getQueue().size());
    }
}
