package com.aionn.ordering.infrastructure.integration;

import com.aionn.ordering.application.port.out.VoucherGateway;
import com.aionn.sharedkernel.integration.port.promotion.VoucherApplyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class VoucherApplyAdapter implements VoucherGateway {

    private final VoucherApplyPort voucherApplyPort;

    @Override
    public Discount apply(String userId, String merchantId, String voucherCode, String orderId,
            BigDecimal lineSubtotal, String currency) {
        VoucherApplyPort.Discount result = voucherApplyPort.apply(userId, merchantId, voucherCode,
                orderId, lineSubtotal, currency);
        return new Discount(result.amount(), result.currency(), result.applied(), result.reason());
    }

    @Override
    public void release(String userId, String orderId, String reason) {
        voucherApplyPort.release(userId, orderId, reason);
    }
}
