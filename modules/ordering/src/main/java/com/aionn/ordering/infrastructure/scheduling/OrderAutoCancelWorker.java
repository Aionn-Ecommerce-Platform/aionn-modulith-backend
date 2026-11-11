package com.aionn.ordering.infrastructure.scheduling;

import java.time.Clock;
import java.util.List;

import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.StockReservationGateway;
import com.aionn.ordering.application.port.out.integration.OrderingIntegrationEventPublisherPort;
import com.aionn.ordering.application.port.out.integration.OrderingIntegrationEventPublisherPort.CancellationKind;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.sharedkernel.application.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCancelWorker {

    private final OrderPersistencePort orderRepository;
    private final StockReservationGateway stockReservationGateway;
    private final EventPublisher eventPublisher;
    private final OrderingIntegrationEventPublisherPort integrationEventPublisher;
    private final Clock clock;

    /** Uses an independent transaction so a single failure does not poison the batch. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelOneExpired(String orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.ORDER_NOT_FOUND));
        order.autoCancel("PAYMENT_TIMEOUT", clock.instant());
        Order saved = orderRepository.save(order);
        eventPublisher.publish(order.pullEvents());
        integrationEventPublisher.publishOrderCancelled(saved.getOrderId(), "PAYMENT_TIMEOUT",
                "Payment timeout", CancellationKind.AUTO_CANCELLED);

        List<OrderItem> items = List.copyOf(order.items());
        if (!org.springframework.transaction.support.TransactionSynchronizationManager
                .isSynchronizationActive()) {
            releaseReservations(items);
            return;
        }
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        releaseReservations(items);
                    }
                });
    }

    private void releaseReservations(List<OrderItem> items) {
        for (OrderItem item : items) {
            if (item.reservationId() == null) {
                continue;
            }
            try {
                stockReservationGateway.release(item.reservationId(), "auto-cancel");
            } catch (RuntimeException ex) {
                log.error("Auto-cancel: reservation {} release failed after commit",
                        item.reservationId(), ex);
            }
        }
    }
}
