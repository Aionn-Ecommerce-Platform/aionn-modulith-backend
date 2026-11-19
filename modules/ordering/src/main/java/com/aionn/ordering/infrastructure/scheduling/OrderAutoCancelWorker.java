package com.aionn.ordering.infrastructure.scheduling;

import com.aionn.contracts.port.promotion.FlashSaleQueryPort;
import com.aionn.ordering.application.port.out.CompensationTaskPort;
import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.application.port.out.StockReservationGateway;
import com.aionn.ordering.application.port.out.integration.OrderingIntegrationEventPublisherPort;
import com.aionn.ordering.domain.exception.OrderingErrorCode;
import com.aionn.ordering.domain.exception.OrderingException;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.sharedkernel.application.port.EventPublisher;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCancelWorker {

    private final OrderPersistencePort orderPersistencePort;
    private final StockReservationGateway stockReservationGateway;
    private final FlashSaleQueryPort flashSaleQueryPort;
    private final EventPublisher eventPublisher;
    private final OrderingIntegrationEventPublisherPort integrationEventPublisher;
    private final CompensationTaskPort compensationTaskPort;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelOneExpired(String orderId) {
        Order order = orderPersistencePort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderingException(OrderingErrorCode.ORDER_NOT_FOUND));
        order.autoCancel("PAYMENT_TIMEOUT", clock.instant());
        Order saved = orderPersistencePort.save(order);
        eventPublisher.publish(order.pullEvents());
        integrationEventPublisher.publishOrderCancelled(saved.getOrderId(), "PAYMENT_TIMEOUT",
                "Payment timeout", OrderingIntegrationEventPublisherPort.CancellationKind.AUTO_CANCELLED);

        List<OrderItem> items = List.copyOf(order.items());
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            releaseReservations(order, items);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        releaseReservations(order, items);
                    }
                });
    }

    private void releaseReservations(Order order, List<OrderItem> items) {
        releaseFlashSaleAllocation(order.getOrderId());
        for (OrderItem item : items) {
            if (item.reservationId() == null) {
                continue;
            }
            try {
                stockReservationGateway.release(item.reservationId(), "auto-cancel");
            } catch (RuntimeException ex) {
                log.error("Auto-cancel: reservation {} release failed after commit",
                        item.reservationId(), ex);
                enqueueRelease(item.reservationId());
            }
        }
    }

    private void releaseFlashSaleAllocation(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return;
        }
        try {
            flashSaleQueryPort.release(orderId);
        } catch (RuntimeException ex) {
            log.error("Auto-cancel: flash-sale allocation release failed for order {}", orderId, ex);
            try {
                compensationTaskPort.enqueue(new CompensationTaskPort.Task(
                        "flash-sale-release:" + orderId,
                        CompensationTaskPort.Type.FLASH_SALE_RELEASE,
                        orderId, null, orderId, "auto-cancel", 0));
            } catch (RuntimeException persistenceFailure) {
                log.error("Unable to persist flash-sale release task for order {}",
                        orderId, persistenceFailure);
            }
        }
    }

    private void enqueueRelease(String reservationId) {
        try {
            compensationTaskPort.enqueue(new CompensationTaskPort.Task(
                    "reservation-release:" + reservationId,
                    CompensationTaskPort.Type.RESERVATION_RELEASE,
                    reservationId, null, null, "auto-cancel", 0));
        } catch (RuntimeException persistenceFailure) {
            log.error("Unable to persist auto-cancel release task for reservation {}",
                    reservationId, persistenceFailure);
        }
    }
}
