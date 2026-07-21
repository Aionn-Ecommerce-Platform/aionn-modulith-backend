package com.aionn.payment.infrastructure.integration;

import com.aionn.payment.application.service.SettlementService;
import com.aionn.payment.infrastructure.integration.listener.SettlementEventListener;
import com.aionn.sharedkernel.integration.event.ordering.OrderApprovedIntegrationEvent;
import com.aionn.sharedkernel.integration.event.ordering.OrderCompletedIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentIntegrationAdaptersTest {

    @Mock
    private SettlementService settlementService;

    private SettlementEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new SettlementEventListener(settlementService);
    }

    @Test
    void shouldHandleSettlementEvents() {
        OrderApprovedIntegrationEvent approvedEvent = new OrderApprovedIntegrationEvent(
                "evt-1", "order-1", "pay-1", Instant.now());

        OrderCompletedIntegrationEvent completedEvent = new OrderCompletedIntegrationEvent(
                "evt-2", "order-1", Instant.now());

        assertDoesNotThrow(() -> {
            listener.on(approvedEvent);
            listener.on(completedEvent);
        });

        verify(settlementService).onOrderApproved("order-1", "pay-1");
        verify(settlementService).onOrderCompleted("order-1");
    }
}
