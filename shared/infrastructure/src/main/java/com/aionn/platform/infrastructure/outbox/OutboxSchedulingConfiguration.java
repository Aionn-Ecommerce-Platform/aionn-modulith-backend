package com.aionn.platform.infrastructure.outbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OutboxProperties.class)
class OutboxSchedulingConfiguration {

    @Bean("outboxTaskScheduler")
    ThreadPoolTaskScheduler outboxTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("outbox-dispatch-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }
}
