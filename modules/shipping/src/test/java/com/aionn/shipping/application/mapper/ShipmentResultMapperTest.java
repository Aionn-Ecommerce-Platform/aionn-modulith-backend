package com.aionn.shipping.application.mapper;

import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentResultMapperTest {

    private final ShipmentResultMapper mapper = Mappers.getMapper(ShipmentResultMapper.class);

    private static final ShipmentAddress ADDRESS = new ShipmentAddress(
            "John Doe", "0912345678", "123 Main", "00001", "001", "01", "VN");
    private static final ShipmentDimensions DIMENSIONS = new ShipmentDimensions(
            500, BigDecimal.valueOf(20), BigDecimal.valueOf(15), BigDecimal.valueOf(10));

    @Test
    void mapsBasicFields() {
        Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");

        ShipmentResult result = mapper.toResult(shipment);

        assertThat(result.shipmentId()).isEqualTo("S_1");
        assertThat(result.orderId()).isEqualTo("ORDER_1");
        assertThat(result.merchantId()).isEqualTo("M_1");
        assertThat(result.userId()).isEqualTo("U_1");
        assertThat(result.shippingFee()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(result.currency()).isEqualTo("VND");
        assertThat(result.attemptCount()).isZero();
    }

    @Test
    void mapsStatusFromEnumToName() {
        Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");

        ShipmentResult result = mapper.toResult(shipment);

        assertThat(result.status()).isEqualTo(ShipmentStatus.REQUESTED.name());
    }

    @Test
    void mapsTrackingFieldsAfterRegistration() {
        Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
        shipment.registerWithCarrier("TRACK_1", "CARRIER_1", null);

        ShipmentResult result = mapper.toResult(shipment);

        assertThat(result.trackingCode()).isEqualTo("TRACK_1");
        assertThat(result.carrierOrderId()).isEqualTo("CARRIER_1");
        assertThat(result.status()).isEqualTo("REGISTERED");
    }
}
