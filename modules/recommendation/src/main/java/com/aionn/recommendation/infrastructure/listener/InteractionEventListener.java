package com.aionn.recommendation.infrastructure.listener;

import com.aionn.recommendation.application.dto.command.RecordInteractionCommand;
import com.aionn.recommendation.application.port.in.RecordInteractionInputPort;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.contracts.event.catalog.ProductViewedIntegrationEvent;
import com.aionn.contracts.event.catalog.SearchPerformedIntegrationEvent;
import com.aionn.contracts.event.ordering.CartItemAddedIntegrationEvent;
import com.aionn.contracts.event.ordering.OrderPlacedIntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingests behavioural signals from other modules. Handlers must declare integration event types, never
 * domain types: only {@code IntegrationEvent}/{@code EventEnvelope} carry an event ID for
 * {@code OutboxConsumerInboxAspect}, so a domain-typed handler would re-apply on every redelivery.
 * {@code REQUIRES_NEW} keeps a failed ingest from rolling back the dispatch that triggered it.
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

    /** Searches carry their facets; a weak signal, so low weight and never used for co-occurrence. */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSearchPerformed(SearchPerformedIntegrationEvent event) {
        recordInteractionInputPort.execute(RecordInteractionCommand.forSearch(
                event.userId(),
                event.occurredAt(),
                event.eventId(),
                event.categoryIds(),
                event.brandId(),
                event.keyword()));
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

    /** Quantity is ignored: buying five says they needed five, not that they like it five times more. */
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
