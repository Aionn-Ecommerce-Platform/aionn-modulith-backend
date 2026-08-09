package com.aionn.shipping.infrastructure.scheduling;

import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.application.port.out.integration.ShippingIntegrationEventPublisherPort;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShipmentStatusApplierTest {

    @Mock
    private ShipmentPersistencePort shipmentRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ShippingIntegrationEventPublisherPort integrationEventPublisher;

    @Mock
    private Shipment shipment;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-08-05T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private ShipmentStatusApplier applier;

    private static CarrierClient.OrderDetail detail() {
        return new CarrierClient.OrderDetail(
                "PICKED_UP", "HUB", "driver", "090", "sig", "reason", "WH_1", null);
    }

    @Test
    void doesNothingWhenShipmentIsMissing() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.empty());

        applier.apply("S_1", ShipmentStatus.PICKED_UP, detail());

        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher, integrationEventPublisher);
    }

    @Test
    void doesNothingWhenCurrentStatusAlreadyMatchesTarget() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getStatus()).thenReturn(ShipmentStatus.PICKED_UP);

        applier.apply("S_1", ShipmentStatus.PICKED_UP, detail());

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenShipmentIsAlreadyTerminal() {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getStatus()).thenReturn(ShipmentStatus.DELIVERED);

        applier.apply("S_1", ShipmentStatus.RETURNED, detail());

        verify(shipmentRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = { "REQUESTED", "REGISTERED" })
    void ignoresTargetStatusesThatHaveNoTransition(ShipmentStatus target) {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getStatus()).thenReturn(ShipmentStatus.IN_TRANSIT);

        applier.apply("S_1", target, detail());

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void appliesPickedUpAndPublishesDispatched() {
        stubTransitionFrom(ShipmentStatus.REGISTERED);

        applier.apply("S_1", ShipmentStatus.PICKED_UP, detail());

        verify(shipment).markPickedUp("WH_1", clock);
        verify(shipmentRepository).save(shipment);
        verify(eventPublisher).publish(anyCollection());
        verify(integrationEventPublisher).publishDispatched(any(), any(), any());
    }

    @Test
    void appliesInTransitWithoutIntegrationEvent() {
        stubTransitionFrom(ShipmentStatus.PICKED_UP);

        applier.apply("S_1", ShipmentStatus.IN_TRANSIT, detail());

        verify(shipment).updateInTransitStatus("HUB", "PICKED_UP", clock);
        verifyNoInteractions(integrationEventPublisher);
    }

    @Test
    void appliesOutForDeliveryWithoutIntegrationEvent() {
        stubTransitionFrom(ShipmentStatus.IN_TRANSIT);

        applier.apply("S_1", ShipmentStatus.OUT_FOR_DELIVERY, detail());

        verify(shipment).markOutForDelivery("driver", "090", clock);
        verifyNoInteractions(integrationEventPublisher);
    }

    @Test
    void appliesDeliveredAndPublishesDelivered() {
        stubTransitionFrom(ShipmentStatus.OUT_FOR_DELIVERY);

        applier.apply("S_1", ShipmentStatus.DELIVERED, detail());

        verify(shipment).markDelivered("sig", clock);
        verify(integrationEventPublisher).publishDelivered(any(), any(), any(), any());
    }

    @Test
    void appliesDeliveryFailureAndPublishesFailure() {
        stubTransitionFrom(ShipmentStatus.OUT_FOR_DELIVERY);

        applier.apply("S_1", ShipmentStatus.DELIVERY_FAILED, detail());

        verify(shipment).recordDeliveryFailure("reason", clock);
        verify(integrationEventPublisher).publishDeliveryFailed(any(), any(), any(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void appliesReturnedWithoutIntegrationEvent() {
        stubTransitionFrom(ShipmentStatus.PICKED_UP);

        applier.apply("S_1", ShipmentStatus.RETURNED, detail());

        verify(shipment).markReturned("reason", clock);
        verifyNoInteractions(integrationEventPublisher);
    }

    @Test
    void appliesCancelledWithoutIntegrationEvent() {
        stubTransitionFrom(ShipmentStatus.REGISTERED);

        applier.apply("S_1", ShipmentStatus.CANCELLED, detail());

        verify(shipment).cancel("reason", clock);
        verifyNoInteractions(integrationEventPublisher);
    }

    private void stubTransitionFrom(ShipmentStatus current) {
        when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
        when(shipment.getStatus()).thenReturn(current);
        when(shipmentRepository.save(shipment)).thenReturn(shipment);
    }
}
