package com.aionn.ordering.infrastructure.scheduling;

import com.aionn.ordering.application.port.out.OrderReturnPersistencePort;
import com.aionn.ordering.application.service.OrderReturnService;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnRefundRetryScheduler {

    private static final int MAX_ATTEMPTS = 5;
    private final OrderReturnPersistencePort returnRepository;
    private final OrderReturnService returnService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${ordering.return-refund.delay-ms:30000}")
    @SchedulerLock(name = "ordering-return-refund", lockAtMostFor = "PT10M", lockAtLeastFor = "PT25S")
    public void run() {
        for (String returnId : returnRepository.findRefundRetryIds(clock.instant(), MAX_ATTEMPTS, 100)) {
            try {
                returnService.retryRefund(returnId);
            } catch (RuntimeException exception) {
                log.warn("Return refund retry failed for {}: {}", returnId, exception.getMessage());
            }
        }
    }
}
