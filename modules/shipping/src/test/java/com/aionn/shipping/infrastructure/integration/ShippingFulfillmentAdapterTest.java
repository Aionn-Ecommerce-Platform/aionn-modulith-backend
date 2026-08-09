package com.aionn.shipping.infrastructure.integration;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.CreateShipmentCommand;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.application.dto.shipment.result.ShipmentResult;
import com.aionn.shipping.application.port.in.shipment.RegisterShipmentInputPort;
import com.aionn.shipping.application.service.ShipmentService;
import com.aionn.shipping.infrastructure.config.ShippingProperties;
import com.aionn.sharedkernel.integration.port.shipping.ShippingFulfillmentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingFulfillmentAdapterTest {

        @Mock
        private ShipmentService shipmentService;

        @Mock
        private RegisterShipmentInputPort registerShipmentInputPort;

        @Mock
        private ShippingProperties properties;

        @Mock
        private ShippingProperties.DefaultDimensions defaultDimensions;

        private ShippingFulfillmentAdapter adapter;

        @BeforeEach
        void setUp() {
                lenient().when(properties.defaultDimensions()).thenReturn(defaultDimensions);
                lenient().when(defaultDimensions.weightGram()).thenReturn(100);
                lenient().when(defaultDimensions.lengthCm()).thenReturn(BigDecimal.TEN);
                lenient().when(defaultDimensions.widthCm()).thenReturn(BigDecimal.TEN);
                lenient().when(defaultDimensions.heightCm()).thenReturn(BigDecimal.TEN);

                adapter = new ShippingFulfillmentAdapter(shipmentService, registerShipmentInputPort, properties);
        }

        @Test
        void quoteCallsServiceAndReturnsResult() {
                ShippingQuoteResult quoteResult = new ShippingQuoteResult(
                                BigDecimal.valueOf(20000), "VND", "HN", "carrier", "desc", null, null);
                when(shipmentService.quote(any(QuoteShippingCommand.class))).thenReturn(quoteResult);

                var address = new ShippingFulfillmentPort.Address(
                                "John", "0912", "Addr", "W_1", "D_1", "P_1", "VN");

                var result = adapter.quote("O_1", "M_1", address, "VND");

                assertThat(result.fee()).isEqualByComparingTo(BigDecimal.valueOf(20000));
                assertThat(result.currency()).isEqualTo("VND");
        }

        @Test
        void createAndRegisterCallsServiceAndReturnsResult() {
                Instant now = Instant.now();
                var addressVal = new com.aionn.shipping.domain.valueobject.ShipmentAddress(
                                "John", "0912", "Addr", "W_1", "D_1", "P_1", "VN");
                var dimensionsVal = new com.aionn.shipping.domain.valueobject.ShipmentDimensions(
                                100, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
                com.aionn.shipping.domain.model.Shipment createdShipment = com.aionn.shipping.domain.model.Shipment
                                .request(
                                                "S_1", "O_1", "M_1", "U_1", addressVal, dimensionsVal,
                                                BigDecimal.ZERO, BigDecimal.valueOf(20000), "VND", java.time.Clock.fixed(java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
                ShipmentResult registeredResult = new ShipmentResult(
                                "S_1", "O_1", "M_1", "U_1", "TR_1", "C_1", "L_1",
                                BigDecimal.ZERO, BigDecimal.valueOf(20000), "VND", "REGISTERED",
                                null, null, null, 0, null, null, null, null, null, null, now, now);

                when(shipmentService.createShipment(any(CreateShipmentCommand.class))).thenReturn(createdShipment);
                when(registerShipmentInputPort.execute("S_1")).thenReturn(registeredResult);

                var address = new ShippingFulfillmentPort.Address(
                                "John", "0912", "Addr", "W_1", "D_1", "P_1", "VN");

                var result = adapter.createAndRegister(
                                "O_1", "M_1", "U_1", address, BigDecimal.ZERO, BigDecimal.valueOf(20000), "VND");

                assertThat(result.shipmentId()).isEqualTo("S_1");
                assertThat(result.trackingCode()).isEqualTo("TR_1");
                assertThat(result.carrierOrderId()).isEqualTo("C_1");
                assertThat(result.labelUrl()).isEqualTo("L_1");
        }
}
