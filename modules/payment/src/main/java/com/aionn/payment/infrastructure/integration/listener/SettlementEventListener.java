package com.aionn.payment.infrastructure.integration.listener;

import com.aionn.payment.application.service.SettlementService;
import com.aionn.sharedkernel.integration.event.ordering.OrderApprovedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCancelledIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCompletedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentRefundedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final SettlementService settlementService;

    @EventListener
    public void on(OrderApprovedIntegrationEvent event) {
        try {
            settlementService.onOrderApproved(event.orderId(), event.paymentId());
        } catch (RuntimeException ex) {
            log.error("Settlement: failed to record SALE for order {}", event.orderId(), ex);
        }
    }

    @EventListener
    public void on(OrderCompletedIntegrationEvent event) {
        try {
            settlementService.onOrderCompleted(event.orderId());
        } catch (RuntimeException ex) {
            log.error("Settlement: failed to move available for order {}", event.orderId(), ex);
        }
    }

    @EventListener
    public void on(OrderCancelledIntegrationEvent event) {
        try {
            settlementService.onOrderCancelled(event.orderId());
        } catch (RuntimeException ex) {
            log.error("Settlement: failed to reverse for cancelled order {}", event.orderId(), ex);
        }
    }

    @EventListener
    public void on(PaymentRefundedIntegrationEvent event) {
        try {
            settlementService.onPaymentRefunded(event.orderId(), event.paymentId(),
                    event.amount(), event.currency());
        } catch (RuntimeException ex) {
            log.error("Settlement: failed to apply refund for payment {}", event.paymentId(), ex);
        }
    }
}
