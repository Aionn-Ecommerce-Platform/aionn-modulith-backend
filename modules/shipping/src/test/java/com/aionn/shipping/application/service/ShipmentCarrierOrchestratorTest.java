package com.aionn.shipping.application.service;

import com.aionn.shipping.application.dto.shipment.command.CancelShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.FetchLabelCommand;
import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentCarrierOrchestratorTest {

    private static final ShipmentAddress ADDRESS = new ShipmentAddress(
            "John Doe", "0912345678", "123 Main", "00001", "001", "HN", "VN");
    private static final ShipmentDimensions DIMENSIONS = new ShipmentDimensions(
            500, BigDecimal.valueOf(20), BigDecimal.valueOf(15), BigDecimal.valueOf(10));

    @Mock
    ShipmentService shipmentService;
    @Mock
    CarrierClient carrierClient;
    @Mock
    MerchantQueryPort merchantQueryPort;

    ShipmentCarrierOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new ShipmentCarrierOrchestrator(shipmentService, carrierClient, merchantQueryPort);
    }

    private static Shipment shipment() {
        return Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
    }

    private static Shipment registeredShipment() {
        Shipment shipment = shipment();
        shipment.registerWithCarrier("TRACK_1", "CARRIER_O1", null);
        shipment.pullEvents();
        return shipment;
    }

    // --- registerWithCarrier --------------------------------------------------

    @Test
    void registerWithCarrierCallsCarrierThenPersistsRegistration() {
        Shipment shipment = shipment();
        Shipment persisted = registeredShipment();
        when(shipmentService.loadShipment("S_1")).thenReturn(shipment);
        CarrierClient.Registration reg = new CarrierClient.Registration("TRACK_1", "CARRIER_O1", null);
        when(carrierClient.register(any(), any(), any(), any(), any(), any(), any())).thenReturn(reg);
        when(shipmentService.applyRegistration("S_1", reg)).thenReturn(persisted);

        Shipment result = orchestrator.registerWithCarrier("S_1");

        assertThat(result).isSameAs(persisted);
        verify(carrierClient).register("S_1", "ORDER_1", ADDRESS, DIMENSIONS,
                BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
    }

    @Test
    void registerWithCarrierIsIdempotentWhenAlreadyRegistered() {
        Shipment shipment = registeredShipment();
        when(shipmentService.loadShipment("S_1")).thenReturn(shipment);

        Shipment result = orchestrator.registerWithCarrier("S_1");

        assertThat(result).isSameAs(shipment);
        verifyNoInteractions(carrierClient);
        verify(shipmentService, never()).applyRegistration(any(), any());
    }

    @Test
    void registerWithCarrierTranslatesRuntimeFailureIntoCarrierError() {
        when(shipmentService.loadShipment("S_1")).thenReturn(shipment());
        when(carrierClient.register(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Carrier unavailable"));

        assertThatThrownBy(() -> orchestrator.registerWithCarrier("S_1"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("Carrier unavailable")
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_CARRIER_ERROR.getCode());

        verify(shipmentService, never()).applyRegistration(any(), any());
    }

    @Test
    void registerWithCarrierRethrowsShippingExceptionUnchanged() {
        when(shipmentService.loadShipment("S_1")).thenReturn(shipment());
        when(carrierClient.register(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "bad address"));

        assertThatThrownBy(() -> orchestrator.registerWithCarrier("S_1"))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void registerWithCarrierPropagatesMissingShipment() {
        when(shipmentService.loadShipment("MISSING"))
                .thenThrow(new ShippingException(ShippingErrorCode.SHIPMENT_NOT_FOUND));

        assertThatThrownBy(() -> orchestrator.registerWithCarrier("MISSING"))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_NOT_FOUND.getCode());

        verifyNoInteractions(carrierClient);
    }

    // --- fetchLabel -----------------------------------------------------------

    @Test
    void fetchLabelStoresCarrierLabelUrl() {
        Shipment shipment = registeredShipment();
        when(shipmentService.loadShipment("S_1")).thenReturn(shipment);
        when(carrierClient.fetchLabel("TRACK_1")).thenReturn("https://label");
        when(shipmentService.applyLabel("S_1", "https://label")).thenReturn(shipment);

        Shipment result = orchestrator.fetchLabel(new FetchLabelCommand("S_1", "U_1"));

        assertThat(result).isSameAs(shipment);
        verify(shipmentService).ensureViewable(shipment, "U_1");
        verify(carrierClient).fetchLabel("TRACK_1");
    }

    @Test
    void fetchLabelRejectsShipmentNotYetRegisteredWithCarrier() {
        when(shipmentService.loadShipment("S_1")).thenReturn(shipment());

        assertThatThrownBy(() -> orchestrator.fetchLabel(new FetchLabelCommand("S_1", "U_1")))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("before carrier registration")
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_INVALID_STATE.getCode());

        verifyNoInteractions(carrierClient);
        verify(shipmentService, never()).applyLabel(any(), any());
    }

    @Test
    void fetchLabelRejectsViewerWhoCannotSeeTheShipment() {
        Shipment shipment = registeredShipment();
        when(shipmentService.loadShipment("S_1")).thenReturn(shipment);
        doThrow(new ShippingException(ShippingErrorCode.SHIPMENT_FORBIDDEN))
                .when(shipmentService).ensureViewable(shipment, "STRANGER");

        assertThatThrownBy(() -> orchestrator.fetchLabel(new FetchLabelCommand("S_1", "STRANGER")))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_FORBIDDEN.getCode());

        verifyNoInteractions(carrierClient);
    }

    @Test
    void fetchLabelPropagatesCarrierFailure() {
        when(shipmentService.loadShipment("S_1")).thenReturn(registeredShipment());
        when(carrierClient.fetchLabel("TRACK_1"))
                .thenThrow(new ShippingException(ShippingErrorCode.SHIPMENT_CARRIER_ERROR, "label token missing"));

        assertThatThrownBy(() -> orchestrator.fetchLabel(new FetchLabelCommand("S_1", "U_1")))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("label token missing");

        verify(shipmentService, never()).applyLabel(any(), any());
    }

    // --- cancelShipment -------------------------------------------------------

    @Test
    void cancelShipmentCancelsAtCarrierThenLocally() {
        Shipment shipment = registeredShipment();
        when(shipmentService.loadShipment("S_1")).thenReturn(shipment);
        when(merchantQueryPort.findMerchantIdByOwnerId("OWNER_1")).thenReturn(Optional.of("M_1"));
        when(shipmentService.applyCancel("S_1", "reason")).thenReturn(shipment);

        Shipment result = orchestrator.cancelShipment(new CancelShipmentCommand("S_1", "reason", "OWNER_1"));

        assertThat(result).isSameAs(shipment);
        verify(carrierClient).cancel("TRACK_1", "reason");
        verify(shipmentService).applyCancel("S_1", "reason");
    }

    @Test
    void cancelShipmentSkipsCarrierWhenNotYetRegistered() {
        Shipment shipment = shipment();
        when(shipmentService.loadShipment("S_1")).thenReturn(shipment);
        when(merchantQueryPort.findMerchantIdByOwnerId("OWNER_1")).thenReturn(Optional.of("M_1"));
        when(shipmentService.applyCancel("S_1", "reason")).thenReturn(shipment);

        orchestrator.cancelShipment(new CancelShipmentCommand("S_1", "reason", "OWNER_1"));

        verifyNoInteractions(carrierClient);
        verify(shipmentService).applyCancel("S_1", "reason");
    }

    @Test
    void cancelShipmentStillCancelsLocallyWhenCarrierCancelFails() {
        Shipment shipment = registeredShipment();
        when(shipmentService.loadShipment("S_1")).thenReturn(shipment);
        when(merchantQueryPort.findMerchantIdByOwnerId("OWNER_1")).thenReturn(Optional.of("M_1"));
        when(shipmentService.applyCancel("S_1", "reason")).thenReturn(shipment);
        doThrow(new RuntimeException("Carrier unavailable")).when(carrierClient).cancel(any(), any());

        orchestrator.cancelShipment(new CancelShipmentCommand("S_1", "reason", "OWNER_1"));

        verify(carrierClient).cancel("TRACK_1", "reason");
        verify(shipmentService).applyCancel("S_1", "reason");
    }

    @Test
    void cancelShipmentRejectsCallerWithoutMerchant() {
        when(shipmentService.loadShipment("S_1")).thenReturn(registeredShipment());
        when(merchantQueryPort.findMerchantIdByOwnerId("OWNER_X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrator.cancelShipment(
                new CancelShipmentCommand("S_1", "reason", "OWNER_X")))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_FORBIDDEN.getCode());

        verifyNoInteractions(carrierClient);
        verify(shipmentService, never()).applyCancel(any(), any());
    }

    @Test
    void cancelShipmentRejectsMerchantThatDoesNotOwnTheShipment() {
        when(shipmentService.loadShipment("S_1")).thenReturn(registeredShipment());
        when(merchantQueryPort.findMerchantIdByOwnerId("OWNER_2")).thenReturn(Optional.of("M_OTHER"));

        assertThatThrownBy(() -> orchestrator.cancelShipment(
                new CancelShipmentCommand("S_1", "reason", "OWNER_2")))
                .isInstanceOf(ShippingException.class);

        verifyNoInteractions(carrierClient);
        verify(shipmentService, never()).applyCancel(any(), any());
    }
}
