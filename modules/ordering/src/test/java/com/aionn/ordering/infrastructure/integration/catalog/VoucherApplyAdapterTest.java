package com.aionn.ordering.infrastructure.integration.catalog;

import com.aionn.ordering.application.port.out.VoucherGateway;
import com.aionn.sharedkernel.integration.port.promotion.VoucherApplyPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherApplyAdapterTest {

    @Mock
    private VoucherApplyPort voucherApplyPort;

    @InjectMocks
    private VoucherApplyAdapter adapter;

    @Test
    void applyDelegatesToVoucherApplyPort() {
        VoucherApplyPort.Discount discount = new VoucherApplyPort.Discount(BigDecimal.TEN, "VND", true, "Applied");
        when(voucherApplyPort.apply("usr-1", "m-1", "VOUCHER", "ord-1", BigDecimal.valueOf(100), "VND"))
                .thenReturn(discount);

        VoucherGateway.Discount result = adapter.apply("usr-1", "m-1", "VOUCHER", "ord-1", BigDecimal.valueOf(100), "VND");

        assertEquals(BigDecimal.TEN, result.amount());
        assertEquals("VND", result.currency());
        assertTrue(result.valid());
        assertEquals("Applied", result.reason());
    }

    @Test
    void releaseDelegatesToVoucherApplyPort() {
        adapter.release("usr-1", "ord-1", "cancelled");

        verify(voucherApplyPort).release("usr-1", "ord-1", "cancelled");
    }
}
