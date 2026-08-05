package com.aionn.ordering.application.service;

import com.aionn.ordering.application.port.out.CompensationTaskPort;
import com.aionn.ordering.application.port.out.StockReservationGateway;
import com.aionn.ordering.application.port.out.VoucherGateway;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompensationRetryService {

    public static final int MAX_ATTEMPTS = 10;
    private final CompensationTaskPort taskPort;
    private final StockReservationGateway stockGateway;
    private final VoucherGateway voucherGateway;
    private final Clock clock;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void retry(String taskId) {
        CompensationTaskPort.Task task = taskPort.findById(taskId).orElse(null);
        if (task == null) return;
        try {
            switch (task.type()) {
                case RESERVATION_RELEASE -> stockGateway.release(task.resourceId(), task.reason());
                case VOUCHER_RELEASE -> voucherGateway.release(task.userId(), task.orderId(), task.reason());
            }
            taskPort.markCompleted(task.taskId());
        } catch (RuntimeException failure) {
            int attempt = task.attempts() + 1;
            long delay = Math.min(3600, 1L << Math.min(attempt, 12));
            taskPort.markFailed(task.taskId(), failure.getMessage(), clock.instant().plusSeconds(delay),
                    attempt >= MAX_ATTEMPTS);
        }
    }
}
