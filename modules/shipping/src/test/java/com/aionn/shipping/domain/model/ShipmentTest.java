package com.aionn.shipping.domain.model;

import com.aionn.shipping.domain.event.ShipmentEvents;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentTest {

    private static final ShipmentAddress ADDRESS = new ShipmentAddress(
            "John Doe", "0912345678", "123 Main", "00001", "001", "01", "VN");
    private static final ShipmentDimensions DIMENSIONS = new ShipmentDimensions(
            500, BigDecimal.valueOf(20), BigDecimal.valueOf(15), BigDecimal.valueOf(10));

    private Shipment newRequested() {
        return Shipment.request("S_1", "ORDER_1", "M_1", "U_1", ADDRESS, DIMENSIONS,
                BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
    }

    @Test
    void requestSetsStatusRequestedAndEmitsEvent() {
        Shipment s = newRequested();

        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.REQUESTED);
        assertThat(s.getOrderId()).isEqualTo("ORDER_1");
        assertThat(s.peekEvents())
                .anyMatch(env -> env.payload() instanceof ShipmentEvents.ShipmentRequested);
    }

    @Test
    void requestRejectsBlankOrderId() {
        assertThatThrownBy(() -> Shipment.request("S_1", " ", "M_1", "U_1",
                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND"))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void registerWithCarrierTransitionsToRegistered() {
        Shipment s = newRequested();
        s.pullEvents();

        s.registerWithCarrier("TRACK_1", "CARRIER_1", Instant.now().plusSeconds(86400));

        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.REGISTERED);
        assertThat(s.getTrackingCode()).isEqualTo("TRACK_1");
        assertThat(s.peekEvents())
                .anyMatch(env -> env.payload() instanceof ShipmentEvents.ShipmentRegistered);
    }

    @Test
    void fetchLabelRequiresRegisteredOrPickedUpStatus() {
        Shipment s = newRequested();

        assertThatThrownBy(() -> s.fetchLabel("https://label"))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_INVALID_STATE.getCode());
    }

    @Test
    void cancelRejectedAfterPickup() {
        Shipment s = newRequested();
        s.registerWithCarrier("TRACK_1", "CARRIER_1", null);
        s.markPickedUp("WH_1");

        assertThatThrownBy(() -> s.cancel("buyer"))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_ALREADY_PICKED_UP.getCode());
    }

    @Test
    void cancelTransitionsToCancelledFromRequested() {
        Shipment s = newRequested();
        s.pullEvents();

        s.cancel("buyer");

        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        assertThat(s.peekEvents())
                .anyMatch(env -> env.payload() instanceof ShipmentEvents.ShipmentCancelled);
    }

    @Test
    void ensureViewableAllowsBuyerAndSeller() {
        Shipment s = newRequested();

        assertThatCode(() -> s.ensureViewableBy("U_1", null)).doesNotThrowAnyException();
        assertThatCode(() -> s.ensureViewableBy(null, "M_1")).doesNotThrowAnyException();
    }

    @Test
    void ensureViewableRejectsThirdParty() {
        Shipment s = newRequested();

        assertThatThrownBy(() -> s.ensureViewableBy("OTHER_USER", "OTHER_MERCHANT"))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_FORBIDDEN.getCode());
    }

    @Test
    void recordDeliveryFailureIncrementsAttemptCount() {
        Shipment s = newRequested();
        s.registerWithCarrier("TRACK_1", "CARRIER_1", null);
        s.markPickedUp("WH_1");
        s.markOutForDelivery("Driver", "0901111222");
        s.pullEvents();

        s.recordDeliveryFailure("buyer-not-home");

        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.DELIVERY_FAILED);
        assertThat(s.getAttemptCount()).isEqualTo(1);
        assertThat(s.getLastFailureReason()).isEqualTo("buyer-not-home");
    }

    @Test
    void markDeliveredEmitsDeliveredEvent() {
        Shipment s = newRequested();
        s.registerWithCarrier("TRACK_1", "CARRIER_1", null);
        s.markPickedUp("WH_1");
        s.markOutForDelivery("Driver", "0901111222");
        s.pullEvents();

        s.markDelivered("https://signature");

        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(s.getDeliveredAt()).isNotNull();
        assertThat(s.peekEvents())
                .anyMatch(env -> env.payload() instanceof ShipmentEvents.ShipmentDelivered);
    }

    @Test
    void ensureOwnedByMerchantAllowsCorrectMerchant() {
        Shipment s = newRequested();

        assertThatCode(() -> s.ensureOwnedByMerchant("M_1")).doesNotThrowAnyException();
    }

    @Test
    void ensureOwnedByMerchantRejectsWrongMerchant() {
        Shipment s = newRequested();
        assertThatThrownBy(() -> s.ensureOwnedByMerchant("WRONG_MERCHANT"))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_FORBIDDEN.getCode());
    }

    @Test
    void fetchLabelWorksOnRegisteredStatus() {
        Shipment s = newRequested();
        s.registerWithCarrier("TRACK_1", "CARRIER_1", null);
        s.fetchLabel("https://label-url");
        assertThat(s.getLabelUrl()).isEqualTo("https://label-url");
    }

    @Test
    void updateInTransitStatusWorksOnPickedUpStatus() {
        Shipment s = newRequested();
        s.registerWithCarrier("TRACK_1", "CARRIER_1", null);
        s.markPickedUp("WH_1");
        s.updateInTransitStatus("HUB_1", "at hub");
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(s.getCurrentLocation()).isEqualTo("HUB_1");
    }

    @Test
    void retryDeliveryWorksOnDeliveryFailure() {
        Shipment s = newRequested();
        s.registerWithCarrier("TRACK_1", "CARRIER_1", null);
        s.markPickedUp("WH_1");
        s.markOutForDelivery("Driver", "090");
        s.recordDeliveryFailure("failed");
        s.retryDelivery();
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.OUT_FOR_DELIVERY);
    }

    @Test
    void markReturnedTransitionsToReturned() {
        Shipment s = newRequested();
        s.registerWithCarrier("TRACK_1", "CARRIER_1", null);
        s.markPickedUp("WH_1");
        s.markReturned("customer rejected");
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.RETURNED);
    }

    @Test
    void resolveIssueSavesDetails() {
        Shipment s = newRequested();
        s.resolveIssue("DELAY", "refunded delivery fee");
        assertThat(s.getIssueType()).isEqualTo("DELAY");
        assertThat(s.getIssueResolution()).isEqualTo("refunded delivery fee");
    }

    @Test
    void isCancellableReturnsTrueForRequested() {
        Shipment s = newRequested();
        assertThat(s.isCancellable()).isTrue();
    }
}
