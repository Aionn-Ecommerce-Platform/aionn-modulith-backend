package com.aionn.promotion.infrastructure.listener;

import com.aionn.promotion.application.dto.voucher.command.VoucherCommands;
import com.aionn.promotion.application.port.in.voucher.GrantVoucherInputPort;
import com.aionn.sharedkernel.integration.event.shipping.ShipmentDeliveredIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LateDeliveryCompensationListenerTest {

    @Mock
    private GrantVoucherInputPort grantVoucherInputPort;

    private LateDeliveryCompensationListener listener;

    @BeforeEach
    void setUp() {
        listener = new LateDeliveryCompensationListener(grantVoucherInputPort);
    }

    @Test
    void onShipmentDelivered_whenLateDelivery_grantsCompensationVoucher() {
        Instant expected = Instant.parse("2026-06-20T10:00:00Z");
        Instant delivered = Instant.parse("2026-06-20T12:00:00Z");

        ShipmentDeliveredIntegrationEvent event = new ShipmentDeliveredIntegrationEvent(
                "evt-1", "ship-1", "order-1", "user-123", "https://signature.png",
                delivered, expected, true, delivered
        );

        listener.onShipmentDelivered(event);

        ArgumentCaptor<VoucherCommands.GrantVoucher> captor =
                ArgumentCaptor.forClass(VoucherCommands.GrantVoucher.class);
        verify(grantVoucherInputPort).execute(captor.capture());

        VoucherCommands.GrantVoucher cmd = captor.getValue();
        assertThat(cmd.userId()).isEqualTo("user-123");
        assertThat(cmd.voucherCode()).isEqualTo(LateDeliveryCompensationListener.LATE_DELIVERY_VOUCHER_CODE);
        assertThat(cmd.reason()).contains("order-1");
    }

    @Test
    void onShipmentDelivered_whenOnTimeDelivery_doesNotGrant() {
        Instant expected = Instant.parse("2026-06-20T14:00:00Z");
        Instant delivered = Instant.parse("2026-06-20T12:00:00Z");

        ShipmentDeliveredIntegrationEvent event = new ShipmentDeliveredIntegrationEvent(
                "evt-2", "ship-2", "order-2", "user-123", "https://signature.png",
                delivered, expected, false, delivered
        );

        listener.onShipmentDelivered(event);

        verify(grantVoucherInputPort, never()).execute(any());
    }

    @Test
    void onShipmentDelivered_whenLateButUserIdMissing_doesNotGrant() {
        Instant expected = Instant.parse("2026-06-20T10:00:00Z");
        Instant delivered = Instant.parse("2026-06-20T12:00:00Z");

        ShipmentDeliveredIntegrationEvent event = new ShipmentDeliveredIntegrationEvent(
                "evt-3", "ship-3", "order-3", null, "https://signature.png",
                delivered, expected, true, delivered
        );

        listener.onShipmentDelivered(event);

        verify(grantVoucherInputPort, never()).execute(any());
    }

    @Test
    void onShipmentDelivered_whenGrantFails_propagatesException() {
        Instant expected = Instant.parse("2026-06-20T10:00:00Z");
        Instant delivered = Instant.parse("2026-06-20T12:00:00Z");

        ShipmentDeliveredIntegrationEvent event = new ShipmentDeliveredIntegrationEvent(
                "evt-4", "ship-4", "order-4", "user-123", "https://signature.png",
                delivered, expected, true, delivered
        );

        when(grantVoucherInputPort.execute(any())).thenThrow(new RuntimeException("database timeout"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> listener.onShipmentDelivered(event));

        verify(grantVoucherInputPort).execute(any());
    }
}

