package com.aionn.notification.infrastructure.scheduling;

import com.aionn.notification.application.service.NotificationDeliveryOrchestrator;
import com.aionn.notification.infrastructure.config.properties.NotificationRetryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * Safety net for notification delivery.
 *
 * <p>Event listeners persist notifications as {@code PENDING} and hand them to
 * {@code notificationTaskExecutor} for immediate delivery. This sweep catches whatever the async path
 * did not finish: rejected submissions, provider outages, restarts mid-delivery and retryable failures.
 * It is the reason the async trigger can be dropped when its queue is saturated without losing a message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.retry", name = "enabled", havingValue = "true")
public class NotificationRetryScheduler {

    private final NotificationDeliveryOrchestrator deliveryOrchestrator;
    private final NotificationRetryProperties retryProperties;

    @Scheduled(fixedDelayString = "${notification.retry.delay-ms:30000}")
    @SchedulerLock(name = "notification-retry", lockAtMostFor = "PT10M", lockAtLeastFor = "PT25S")
    public void run() {
        try {
            int succeeded = deliveryOrchestrator.retryPending(retryProperties.batchSize());
            if (succeeded > 0) {
                log.info("Notification retry succeeded for {} message(s)", succeeded);
            }
        } catch (Exception ex) {
            log.error("Notification retry sweep failed", ex);
        }
    }
}
