package com.aionn.ordering.infrastructure.integration;

import com.aionn.ordering.domain.event.OrderEvents;
import com.aionn.sharedkernel.integration.publisher.IntegrationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderIntegrationEventPublisher {

    private final IntegrationEventPublisher integrationEventPublisher;
    private final OrderIntegrationEventMapper mapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderPlaced(OrderEvents.OrderPlaced event) {
        log.debug("Publishing OrderPlacedIntegrationEvent for order: {}", event.orderId());
        var integrationEvent = mapper.toIntegrationEvent(event);
        integrationEventPublisher.publish(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderApproved(OrderEvents.OrderApproved event) {
        log.debug("Publishing OrderApprovedIntegrationEvent for order: {}", event.orderId());
        var integrationEvent = mapper.toIntegrationEvent(event);
        integrationEventPublisher.publish(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderShipped(OrderEvents.OrderShipped event) {
        log.debug("Publishing OrderShippedIntegrationEvent for order: {}", event.orderId());
        var integrationEvent = mapper.toIntegrationEvent(event);
        integrationEventPublisher.publish(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCompleted(OrderEvents.OrderCompleted event) {
        log.debug("Publishing OrderCompletedIntegrationEvent for order: {}", event.orderId());
        var integrationEvent = mapper.toIntegrationEvent(event);
        integrationEventPublisher.publish(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderCancelled(OrderEvents.OrderCancelled event) {
        log.debug("Publishing OrderCancelledIntegrationEvent for order: {}", event.orderId());
        var integrationEvent = mapper.toIntegrationEvent(event);
        integrationEventPublisher.publish(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderAutoCancelled(OrderEvents.OrderAutoCancelled event) {
        log.debug("Publishing OrderCancelledIntegrationEvent (auto) for order: {}", event.orderId());
        var integrationEvent = mapper.toIntegrationEvent(event);
        integrationEventPublisher.publish(integrationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderRejectedByMerchant(OrderEvents.OrderRejectedByMerchant event) {
        log.debug("Publishing OrderCancelledIntegrationEvent (rejected) for order: {}", event.orderId());
        var integrationEvent = mapper.toIntegrationEvent(event);
        integrationEventPublisher.publish(integrationEvent);
    }
}
