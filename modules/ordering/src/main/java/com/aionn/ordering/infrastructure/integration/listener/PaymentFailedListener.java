package com.aionn.ordering.infrastructure.integration.listener;

import com.aionn.ordering.application.service.OrderService;
import com.aionn.sharedkernel.integration.event.payment.PaymentFailedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedListener {

    private final OrderService orderService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PaymentFailedIntegrationEvent event) {
        try {
            orderService.cancelOnPaymentFailure(event.orderId(), event.errorCode(), event.reason());
        } catch (RuntimeException ex) {
            log.error("Could not cancel order {} after payment {} failed (auto-cancel scheduler will retry)",
                    event.orderId(), event.paymentId(), ex);
        }
    }
}
