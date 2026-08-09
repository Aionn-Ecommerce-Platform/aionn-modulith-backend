package com.aionn.ordering.infrastructure.scheduling;

import com.aionn.ordering.application.port.out.CompensationTaskPort;
import com.aionn.ordering.application.service.CompensationRetryService;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompensationRetryScheduler {

    private final CompensationTaskPort taskPort;
    private final CompensationRetryService retryService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${ordering.compensation.delay-ms:30000}")
    @SchedulerLock(name = "ordering-compensation", lockAtMostFor = "PT10M", lockAtLeastFor = "PT25S")
    public void run() {
        for (String taskId : taskPort.findRetryableIds(
                clock.instant(), CompensationRetryService.MAX_ATTEMPTS, 100)) {
            retryService.retry(taskId);
        }
    }
}
