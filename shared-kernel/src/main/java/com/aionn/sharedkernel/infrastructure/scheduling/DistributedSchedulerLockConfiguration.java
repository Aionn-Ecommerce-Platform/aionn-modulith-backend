package com.aionn.sharedkernel.infrastructure.scheduling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.ExtensibleLockProvider;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.support.KeepAliveLockProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;


@Configuration(proxyBeanMethods = false)
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class DistributedSchedulerLockConfiguration {

    // Renewal must not queue behind the business job whose lease it is keeping alive.
    @Bean(destroyMethod = "shutdown")
    ScheduledExecutorService schedulerLockExtensionExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "shedlock-extension-");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    LockProvider schedulerLockProvider(
            DataSource dataSource,
            @Qualifier("schedulerLockExtensionExecutor") ScheduledExecutorService schedulerLockExtensionExecutor) {
        ExtensibleLockProvider jdbcProvider = new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
        return new KeepAliveLockProvider(jdbcProvider, schedulerLockExtensionExecutor);
    }
}
