package com.aionn.ordering.infrastructure.integration.listener;

import com.aionn.ordering.application.dto.order.command.ConfirmDeliveredCommand;
import com.aionn.ordering.application.service.OrderService;
import com.aionn.ordering.domain.valueobject.OrderStatus;
import com.aionn.sharedkernel.integration.event.payment.PaymentCapturedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.payment.PaymentFailedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDeliveredIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void paymentCapturedListenerApprovesPayment() {
        PaymentCapturedIntegrationEvent event = new PaymentCapturedIntegrationEvent(
                "ev-1", "pay-100", "ord-1", "tx-1", BigDecimal.TEN, "VND", Instant.now());

        paymentCapturedListener.on(event);

        verify(orderService).approvePayment("ord-1", "pay-100");
    }

    @Test
    void paymentCapturedListenerHandlesErrorGracefully() {
        PaymentCapturedIntegrationEvent event = new PaymentCapturedIntegrationEvent(
                "ev-1", "pay-100", "ord-1", "tx-1", BigDecimal.TEN, "VND", Instant.now());
        doThrow(new RuntimeException("Error")).when(orderService).approvePayment("ord-1", "pay-100");

        paymentCapturedListener.on(event);

        verify(orderService).approvePayment("ord-1", "pay-100");
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
        ConfirmDeliveredCommand command = new ConfirmDeliveredCommand("ord-1");
        ShipmentDeliveredIntegrationEvent event = new ShipmentDeliveredIntegrationEvent(
                "ev-1", "ship-100", "ord-1", "http://sig", Instant.now(), Instant.now());

        when(orderService.statusOf("ord-1")).thenReturn(OrderStatus.SHIPPED);

        shipmentDeliveredListener.on(event);

        verify(orderService).complete(command);
    }
}
