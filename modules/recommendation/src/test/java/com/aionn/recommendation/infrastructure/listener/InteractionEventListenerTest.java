package com.aionn.recommendation.infrastructure.listener;

import com.aionn.recommendation.application.dto.command.RecordInteractionCommand;
import com.aionn.recommendation.application.port.in.RecordInteractionInputPort;
import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import com.aionn.sharedkernel.integration.event.catalog.ProductViewedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.CartItemAddedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderPlacedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.EventListener;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InteractionEventListenerTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-09-05T11:00:00Z");

    @Mock
    private RecordInteractionInputPort recordInteraction;

    private InteractionEventListener listener() {
        return new InteractionEventListener(recordInteraction);
    }

    @Test
    void aViewCarriesItsClassificationSoNoCatalogLookupIsNeeded() {
        listener().onProductViewed(new ProductViewedIntegrationEvent(
                "evt-view", "p-1", "user-1", "brand-apple", List.of("cat-phone"), OCCURRED_AT));

        RecordInteractionCommand command = captureOne();
        assertThat(command.productId()).isEqualTo("p-1");
        assertThat(command.skuId()).isNull();
        assertThat(command.type()).isEqualTo(InteractionType.VIEW);
        assertThat(command.categoryIds()).containsExactly("cat-phone");
        assertThat(command.brandId()).isEqualTo("brand-apple");
    }

    @Test
    void aCartAddArrivesSkuScopedAndIsLeftForTheServiceToResolve() {
        listener().onCartItemAdded(new CartItemAddedIntegrationEvent(
                "evt-cart", "user-1", "cart-1", "sku-1", 2, OCCURRED_AT));

        RecordInteractionCommand command = captureOne();
        assertThat(command.skuId()).isEqualTo("sku-1");
        assertThat(command.productId()).isNull();
        assertThat(command.type()).isEqualTo(InteractionType.CART_ADD);
    }

    @Test
    void everyOrderLineBecomesOnePurchaseRegardlessOfQuantity() {
        // Buying five of something says the user needed five, not that they like it
        // five times more.
        listener().onOrderPlaced(orderWith(orderLine("sku-1", 5), orderLine("sku-2", 1)));

        ArgumentCaptor<RecordInteractionCommand> captor = ArgumentCaptor.forClass(RecordInteractionCommand.class);
        verify(recordInteraction, times(2)).execute(captor.capture());
        assertThat(captor.getAllValues()).extracting(RecordInteractionCommand::skuId)
                .containsExactly("sku-1", "sku-2");
        assertThat(captor.getAllValues())
                .allSatisfy(command -> assertThat(command.type()).isEqualTo(InteractionType.PURCHASE));
    }

    @Test
    void theSourceEventIdTravelsWithTheCommand() {
        // The ingest path logs against it, and it is the only handle on the originating
        // event once the
        // interaction row exists.
        listener().onOrderPlaced(orderWith(orderLine("sku-1", 1)));

        assertThat(captureOne().sourceEventId()).isEqualTo("evt-order");
    }

    @Test
    void theOccurredAtOfTheSourceEventIsPreservedRatherThanTheIngestTime() {
        // Decay is measured from when the user acted; using ingest time would make a
        // replayed backlog
        // look like a burst of fresh interest.
        listener().onCartItemAdded(new CartItemAddedIntegrationEvent(
                "evt-cart", "user-1", "cart-1", "sku-1", 1, OCCURRED_AT));

        assertThat(captureOne().occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void everyHandlerIsDeclaredAgainstAnIntegrationEvent() {
        // The outbox dispatcher publishes integration payloads raw but wraps domain
        // payloads in an
        // EventEnvelope, so a handler declared against a domain type would never fire.
        // It is also what
        // lets OutboxConsumerInboxAspect read the event ID and make redelivery
        // idempotent - without it
        // a retried order would be counted as a second purchase.
        List<Method> handlers = Arrays.stream(InteractionEventListener.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(EventListener.class))
                .toList();

        assertThat(handlers)
                .hasSize(3)
                .allSatisfy(method -> {
                    assertThat(method.getParameterCount()).isEqualTo(1);
                    assertThat(IntegrationEvent.class).isAssignableFrom(method.getParameterTypes()[0]);
                });
    }

    private RecordInteractionCommand captureOne() {
        ArgumentCaptor<RecordInteractionCommand> captor = ArgumentCaptor.forClass(RecordInteractionCommand.class);
        verify(recordInteraction).execute(captor.capture());
        return captor.getValue();
    }

    private static OrderPlacedIntegrationEvent orderWith(
            OrderPlacedIntegrationEvent.OrderLineItem... items) {
        return new OrderPlacedIntegrationEvent(
                "evt-order",
                "order-1",
                "user-1",
                "merchant-1",
                null,
                List.of(items),
                BigDecimal.valueOf(1_000_000),
                "VND",
                "addr-1",
                "pm-1",
                OCCURRED_AT);
    }

    private static OrderPlacedIntegrationEvent.OrderLineItem orderLine(String skuId, int quantity) {
        return new OrderPlacedIntegrationEvent.OrderLineItem(
                skuId, quantity, BigDecimal.valueOf(500_000), "wh-1", "res-1");
    }
}
