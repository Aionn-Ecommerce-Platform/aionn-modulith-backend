package com.aionn.ordering.application.service;

import com.aionn.ordering.application.port.out.CompensationTaskPort;
import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.PaymentGateway;
import com.aionn.ordering.application.port.out.StockReservationGateway;
import com.aionn.ordering.application.port.out.VoucherGateway;
import com.aionn.ordering.application.policy.CompensationRetryPolicy;
import com.aionn.contracts.port.promotion.FlashSaleQueryPort;
import com.aionn.sharedkernel.util.RetryBackoff;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompensationRetryService {

    private final CompensationTaskPort taskPort;
    private final StockReservationGateway stockGateway;
    private final VoucherGateway voucherGateway;
    private final PaymentGateway paymentGateway;
    private final OrderPersistencePort orderPersistencePort;
    private final FlashSaleQueryPort flashSaleQueryPort;
    private final CompensationRetryPolicy retryPolicy;
    private final Clock clock;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void retry(String taskId) {
        CompensationTaskPort.Task task = taskPort.findById(taskId).orElse(null);
        if (task == null) return;
        try {
            switch (task.type()) {
                case RESERVATION_RELEASE -> stockGateway.release(task.resourceId(), task.reason());
                case VOUCHER_RELEASE -> voucherGateway.release(task.userId(), task.orderId(), task.reason());
                case FLASH_SALE_RELEASE -> flashSaleQueryPort.release(task.resourceId());
                case STOCK_COMMIT -> stockGateway.commit(task.resourceId());
                case REFUND -> {
                    var order = orderPersistencePort.findById(task.orderId()).orElseThrow();
                    if (task.resourceId() != null) {
                        paymentGateway.refund(task.resourceId(), order.getTotalAmount().amount(),
                                order.getCurrency(), task.reason(),
                                "order:" + order.getOrderId() + ":refund:" + task.resourceId());
                    }
                }
            }
            taskPort.markCompleted(task.taskId());
        } catch (RuntimeException failure) {
            int attempt = task.attempts() + 1;
            long delay = RetryBackoff.seconds(attempt);
            taskPort.markFailed(task.taskId(), failure.getMessage(), clock.instant().plusSeconds(delay),
                    attempt >= retryPolicy.maxAttempts());
        }
    }
}
