package com.aionn.recommendation.infrastructure.listener;

import com.aionn.recommendation.application.dto.command.RecordInteractionCommand;
import com.aionn.recommendation.application.port.in.RecordInteractionInputPort;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.contracts.event.catalog.ProductViewedIntegrationEvent;
import com.aionn.contracts.event.catalog.SearchPerformedIntegrationEvent;
import com.aionn.contracts.event.catalog.WishlistItemAddedIntegrationEvent;
import com.aionn.contracts.event.ordering.CartItemAddedIntegrationEvent;
import com.aionn.contracts.event.ordering.OrderPlacedIntegrationEvent;
import com.aionn.contracts.event.payment.PaymentPaidIntegrationEvent;
import com.aionn.contracts.port.ordering.OrderQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionEventListener {

    private final RecordInteractionInputPort recordInteractionInputPort;
    private final OrderQueryPort orderQueryPort;

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
    public void onWishlistItemAdded(WishlistItemAddedIntegrationEvent event) {
        recordInteractionInputPort.execute(RecordInteractionCommand.forProduct(
                event.userId(),
                event.productId(),
                InteractionType.WISHLIST_ADD,
                event.occurredAt(),
                event.eventId(),
                List.of(),
                null));
    }

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

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderPlaced(OrderPlacedIntegrationEvent event) {
        for (OrderPlacedIntegrationEvent.OrderLineItem item : event.items()) {
            recordInteractionInputPort.execute(RecordInteractionCommand.forSku(
                    event.userId(),
                    item.skuId(),
                    InteractionType.ORDER_PLACED,
                    event.occurredAt(),
                    event.eventId()));
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentPaid(PaymentPaidIntegrationEvent event) {
        orderQueryPort.findOrderPurchase(event.orderId()).ifPresent(purchase -> {
            for (String skuId : purchase.skuIds()) {
                recordInteractionInputPort.execute(RecordInteractionCommand.forSku(
                        purchase.userId(),
                        skuId,
                        InteractionType.PURCHASE,
                        event.occurredAt(),
                        event.eventId()));
            }
        });
    }
}
