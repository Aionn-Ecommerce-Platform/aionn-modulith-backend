package com.aionn.recommendation.infrastructure.listener;

import com.aionn.recommendation.application.dto.command.RecordInteractionCommand;
import com.aionn.recommendation.application.port.in.RecordInteractionInputPort;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.sharedkernel.integration.event.catalog.ProductViewedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.CartItemAddedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderPlacedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingests behavioural signals from other modules.
 *
 * <p>Every handler takes an integration event, never a domain event. The dispatcher publishes a domain
 * event twice - once as an {@code EventEnvelope} and once as the bare payload - so a handler declared
 * against a domain type does fire; it just receives a payload carrying no event ID.
 * {@code OutboxConsumerInboxAspect} reads an ID only from an {@code IntegrationEvent} or an
 * {@code EventEnvelope}, so such a handler would run again on every redelivery and apply its side effect
 * twice. Declaring the integration type is what gives each handler its own inbox row, which is what makes
 * at-least-once delivery safe here.
 *
 * <p>{@code REQUIRES_NEW} keeps a failed ingest from rolling back whatever else the dispatch triggered:
 * losing one interaction must not fail an order.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionEventListener {

    private final RecordInteractionInputPort recordInteractionInputPort;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onProductViewed(ProductViewedIntegrationEvent event) {
        recordInteractionInputPort.execute(RecordInteractionCommand.forProduct(
                event.userId(),
                event.productId(),
                InteractionType.VIEW,
                event.occurredAt(),
                event.eventId(),
                event.categoryIds(),
                event.brandId()));
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCartItemAdded(CartItemAddedIntegrationEvent event) {
        recordInteractionInputPort.execute(RecordInteractionCommand.forSku(
                event.userId(),
                event.skuId(),
                InteractionType.CART_ADD,
                event.occurredAt(),
                event.eventId()));
    }

    /**
     * Order lines are recorded as purchases. Quantity is ignored: buying five of something says the
     * user needed five, not that they like it five times more, and letting quantity scale the weight
     * would make bulk purchases dominate every profile.
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderPlaced(OrderPlacedIntegrationEvent event) {
        for (OrderPlacedIntegrationEvent.OrderLineItem item : event.items()) {
            recordInteractionInputPort.execute(RecordInteractionCommand.forSku(
                    event.userId(),
                    item.skuId(),
                    InteractionType.PURCHASE,
                    event.occurredAt(),
                    event.eventId()));
        }
    }
}
