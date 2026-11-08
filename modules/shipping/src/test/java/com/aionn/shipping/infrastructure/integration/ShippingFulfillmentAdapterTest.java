package com.aionn.shipping.infrastructure.integration;

import static com.aionn.sharedkernel.testing.fixture.TestClocks.FIXED;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.port.in.shipment.RegisterShipmentInputPort;
import com.aionn.shipping.application.service.ShipmentService;
import com.aionn.sharedkernel.integration.port.shipping.ShippingFulfillmentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingFulfillmentAdapterTest {

        @Mock
        private ShipmentService shipmentService;

        @Mock
        private RegisterShipmentInputPort registerShipmentInputPort;

        private ShippingFulfillmentAdapter adapter;

        private static List<ShippingFulfillmentPort.Parcel> parcels() {
                return List.of(new ShippingFulfillmentPort.Parcel(
                                500, BigDecimal.valueOf(15), BigDecimal.valueOf(10), BigDecimal.valueOf(5), 1));
        }

        @BeforeEach
        void setUp() {
                adapter = new ShippingFulfillmentAdapter(shipmentService, registerShipmentInputPort);
        }

        @Test
        void quoteCallsServiceAndReturnsResult() {
                ShippingQuoteResult quoteResult = new ShippingQuoteResult(
                                BigDecimal.valueOf(20000), "VND", "HN", "carrier", "desc", null, null);
                when(shipmentService.quote(any(QuoteShippingCommand.class))).thenReturn(quoteResult);

                var address = new ShippingFulfillmentPort.Address(
                                "John", "0912", "Addr", "W_1", "D_1", "P_1", "VN");

                var result = adapter.quote("O_1", "M_1", address, "VND", parcels(), null);

                assertThat(result.fee()).isEqualByComparingTo(BigDecimal.valueOf(20000));
                assertThat(result.currency()).isEqualTo("VND");
        }

        @Test
        void createAndRegisterCallsServiceAndReturnsResult() {
                Instant now = Instant.now();
                var addressVal = new com.aionn.shipping.domain.valueobject.ShipmentAddress(
                                "John", "0912", "Addr", "W_1", "D_1", "P_1", "VN");
                var dimensionsVal = new com.aionn.shipping.domain.valueobject.ShipmentDimensions(
                                500, BigDecimal.valueOf(15), BigDecimal.valueOf(10), BigDecimal.valueOf(5));
                com.aionn.shipping.domain.model.Shipment createdShipment = com.aionn.shipping.domain.model.Shipment
                                .request(
                                                "S_1", "O_1", "M_1", "U_1", addressVal, dimensionsVal,
                                                BigDecimal.ZERO, BigDecimal.valueOf(20000), "VND", FIXED);
                ShipmentResult registeredResult = new ShipmentResult(
                                "S_1", "O_1", "M_1", "U_1", "TR_1", "C_1", "L_1",
                                BigDecimal.ZERO, BigDecimal.valueOf(20000), "VND", "REGISTERED",
                                null, null, null, 0, null, null, null, null, null, null, now, now);

                when(shipmentService.createShipmentsPerConsignment(any(CreateShipmentCommand.class)))
                                .thenReturn(List.of(createdShipment));
                when(registerShipmentInputPort.executeByMerchant("S_1", "M_1")).thenReturn(registeredResult);

                var address = new ShippingFulfillmentPort.Address(
                                "John", "0912", "Addr", "W_1", "D_1", "P_1", "VN");

                var result = adapter.createAndRegister(
                                "O_1", "M_1", "U_1", address, BigDecimal.ZERO, BigDecimal.valueOf(20000), "VND",
                                parcels());

                assertThat(result).hasSize(1);
                assertThat(result.get(0).shipmentId()).isEqualTo("S_1");
                assertThat(result.get(0).trackingCode()).isEqualTo("TR_1");
                assertThat(result.get(0).carrierOrderId()).isEqualTo("C_1");
                assertThat(result.get(0).labelUrl()).isEqualTo("L_1");
        }
}
