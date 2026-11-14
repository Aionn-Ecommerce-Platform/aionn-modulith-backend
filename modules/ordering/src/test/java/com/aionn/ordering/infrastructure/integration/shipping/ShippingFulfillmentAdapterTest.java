package com.aionn.ordering.infrastructure.integration.shipping;

import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.contracts.port.shipping.ShippingFulfillmentPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingFulfillmentAdapterTest {

    @Mock
    private ShippingFulfillmentPort shippingFulfillmentPort;

    @InjectMocks
    private ShippingFulfillmentAdapter adapter;

    private static List<ShippingGateway.Parcel> parcels() {
        return List.of(new ShippingGateway.Parcel(500, null, null, null, 1));
    }

    @Test
    void quoteDelegatesToPort() {
        when(shippingFulfillmentPort.quote(anyString(), anyString(), any(), anyString(), anyList(), any()))
                .thenReturn(new ShippingFulfillmentPort.QuoteResult(BigDecimal.TEN, "VND"));

        ShippingAddress address = new ShippingAddress("fn", "ln", "ph", "st", "ct", "stt", "zp", "cn");
        ShippingGateway.ShippingQuote quote = adapter.quote("ord-1", "m-1", address, "VND", parcels(), null);

        assertNotNull(quote);
        assertEquals(BigDecimal.TEN, quote.fee());
    }

    @Test
    void quoteHandlesNullAddress() {
        when(shippingFulfillmentPort.quote(anyString(), anyString(), any(), anyString(), anyList(), any()))
                .thenReturn(new ShippingFulfillmentPort.QuoteResult(BigDecimal.TEN, "VND"));

        ShippingGateway.ShippingQuote quote = adapter.quote("ord-1", "m-1", null, "VND", parcels(), null);

        assertNotNull(quote);
    }

    @Test
    void createAndRegisterDelegatesToPort() {
        when(shippingFulfillmentPort.createAndRegister(anyString(), anyString(), anyString(), any(), any(), any(),
                anyString(), anyList()))
                .thenReturn(List.of(new ShippingFulfillmentPort.RegistrationResult("ship-1", "track-1", "carrier-ord-1", "http://label")));

        ShippingAddress address = new ShippingAddress("fn", "ln", "ph", "st", "ct", "stt", "zp", "cn");
        List<ShippingGateway.Registration> regs = adapter.createAndRegister("ord-1", "m-1", "usr-1", address,
                BigDecimal.ZERO, BigDecimal.TEN, "VND", parcels());

        assertNotNull(regs);
        assertEquals("ship-1", regs.get(0).shipmentId());
    }

    @Test
    void hasPickedUpShipmentDelegatesToPort() {
        when(shippingFulfillmentPort.hasPickedUpShipment("ord-1")).thenReturn(true);

        assertTrue(adapter.hasPickedUpShipment("ord-1"));
    }
}
