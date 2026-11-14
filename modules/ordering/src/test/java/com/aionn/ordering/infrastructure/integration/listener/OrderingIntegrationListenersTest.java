package com.aionn.ordering.infrastructure.integration.listener;

import com.aionn.ordering.application.service.OrderService;
import com.aionn.contracts.event.payment.PaymentCapturedIntegrationEvent;
import com.aionn.contracts.event.payment.PaymentFailedIntegrationEvent;
import com.aionn.contracts.event.shipping.ShipmentDeliveredIntegrationEvent;
import com.aionn.contracts.event.shipping.ShipmentDispatchedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class OrderingIntegrationListenersTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentCapturedListener paymentCapturedListener;

    @InjectMocks
    private PaymentFailedListener paymentFailedListener;

    @InjectMocks
    private ShipmentDeliveredListener shipmentDeliveredListener;

    @InjectMocks
    private ShipmentDispatchedListener shipmentDispatchedListener;

    @Test
    void paymentCapturedListenerApprovesPayment() {
        PaymentCapturedIntegrationEvent event = new PaymentCapturedIntegrationEvent(
                "ev-1", "pay-100", "ord-1", "tx-1", BigDecimal.TEN, "VND", Instant.now());

        paymentCapturedListener.on(event);

        verify(orderService).approvePayment("ord-1", "pay-100", BigDecimal.TEN, "VND");
    }

    @Test
    void paymentCapturedListenerPropagatesFailureForOutboxRetry() {
        PaymentCapturedIntegrationEvent event = new PaymentCapturedIntegrationEvent(
                "ev-1", "pay-100", "ord-1", "tx-1", BigDecimal.TEN, "VND", Instant.now());
        doThrow(new RuntimeException("Error")).when(orderService)
                .approvePayment("ord-1", "pay-100", BigDecimal.TEN, "VND");

        assertThrows(RuntimeException.class, () -> paymentCapturedListener.on(event));

        verify(orderService).approvePayment("ord-1", "pay-100", BigDecimal.TEN, "VND");
    }

    @Test
    void paymentFailedListenerCancelsOrder() {
        PaymentFailedIntegrationEvent event = new PaymentFailedIntegrationEvent(
                "ev-1", "pay-100", "ord-1", "CARD_DECLINED", "Insufficient funds", Instant.now());

        paymentFailedListener.on(event);

        verify(orderService).cancelOnPaymentFailure("ord-1", "CARD_DECLINED", "Insufficient funds");
    }

    @Test
    void shipmentDeliveredListenerCompletesOrder() {
        ShipmentDeliveredIntegrationEvent event = new ShipmentDeliveredIntegrationEvent(
                "ev-1", "ship-100", "ord-1", "http://sig", Instant.now(), Instant.now());

        shipmentDeliveredListener.on(event);

        verify(orderService).completeFromCarrier("ord-1", "ship-100");
    }

    @Test
    void shipmentDispatchedListenerMarksOrderShipped() {
        ShipmentDispatchedIntegrationEvent event = new ShipmentDispatchedIntegrationEvent(
                "ev-1", "ship-100", "ord-1", "TRACK-1", Instant.now());

        shipmentDispatchedListener.on(event);

        verify(orderService).markShippedFromCarrier("ord-1", "ship-100");
    }
}
