package com.aionn.notification.infrastructure.scheduling;

import com.aionn.notification.application.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.retry", name = "enabled", havingValue = "true")
public class NotificationRetryScheduler {

    private final NotificationDispatchService dispatchService;

    @Value("${notification.retry.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${notification.retry.delay-ms:30000}")
    public void run() {
        try {
            int succeeded = dispatchService.retryPending(batchSize);
            if (succeeded > 0) {
                log.info("Notification retry succeeded for {} message(s)", succeeded);
            }
        } catch (Exception ex) {
            log.error("Notification retry sweep failed", ex);
        }
    }
}
