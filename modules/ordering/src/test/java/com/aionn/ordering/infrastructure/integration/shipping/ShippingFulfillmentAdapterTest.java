package com.aionn.ordering.infrastructure.integration.shipping;

import com.aionn.ordering.application.port.out.ShippingGateway;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.integration.port.shipping.ShippingFulfillmentPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingFulfillmentAdapterTest {

    @Mock
    private ShippingFulfillmentPort shippingFulfillmentPort;

    @InjectMocks
    private ShippingFulfillmentAdapter adapter;

    @Test
    void quoteDelegatesToPort() {
        when(shippingFulfillmentPort.quote(anyString(), anyString(), any(), anyString()))
                .thenReturn(new ShippingFulfillmentPort.QuoteResult(BigDecimal.TEN, "VND"));

        ShippingAddress address = new ShippingAddress("fn", "ln", "ph", "st", "ct", "stt", "zp", "cn");
        ShippingGateway.ShippingQuote quote = adapter.quote("ord-1", "m-1", address, "VND");

        assertNotNull(quote);
        assertEquals(BigDecimal.TEN, quote.fee());
    }

    @Test
    void quoteHandlesNullAddress() {
        when(shippingFulfillmentPort.quote(anyString(), anyString(), any(), anyString()))
                .thenReturn(new ShippingFulfillmentPort.QuoteResult(BigDecimal.TEN, "VND"));

        ShippingGateway.ShippingQuote quote = adapter.quote("ord-1", "m-1", null, "VND");

        assertNotNull(quote);
    }

    @Test
    void createAndRegisterDelegatesToPort() {
        when(shippingFulfillmentPort.createAndRegister(anyString(), anyString(), anyString(), any(), any(), any(), anyString()))
                .thenReturn(new ShippingFulfillmentPort.RegistrationResult("ship-1", "track-1", "carrier-ord-1", "http://label"));

        ShippingAddress address = new ShippingAddress("fn", "ln", "ph", "st", "ct", "stt", "zp", "cn");
        ShippingGateway.Registration reg = adapter.createAndRegister("ord-1", "m-1", "usr-1", address, BigDecimal.ZERO, BigDecimal.TEN, "VND");

        assertNotNull(reg);
        assertEquals("ship-1", reg.shipmentId());
    }
}
