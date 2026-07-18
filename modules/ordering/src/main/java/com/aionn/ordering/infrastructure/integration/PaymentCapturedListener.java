package com.aionn.ordering.infrastructure.integration;

import com.aionn.ordering.application.service.OrderService;
import com.aionn.sharedkernel.integration.event.payment.PaymentCapturedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCapturedListener {

    private final OrderService orderService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PaymentCapturedIntegrationEvent event) {
        try {
            orderService.approvePayment(event.orderId(), event.paymentId());
        } catch (RuntimeException ex) {
            log.error("Could not approve order {} after payment {} was captured",
                    event.orderId(), event.paymentId(), ex);
        }
    }
}
