package com.aionn.payment.infrastructure.persistence.mapper;

import com.aionn.payment.domain.model.Payment;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.infrastructure.persistence.entity.PaymentEntity;
import com.aionn.payment.infrastructure.persistence.entity.PaymentMethodEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceMappersTest {

    private final PaymentDomainMapper paymentMapper = new PaymentDomainMapper();
    private final PaymentMethodDomainMapper methodMapper = new PaymentMethodDomainMapper();

    @Test
    void paymentDomainMapperShouldConvertEntityAndDomain() {
        PaymentEntity entity = PaymentEntity.builder()
                .paymentId("pay-1")
                .orderId("order-1")
                .userId("user-1")
                .paymentMethodId("pm-1")
                .amount(BigDecimal.TEN)
                .currency("VND")
                .gateway("STRIPE")
                .idempotencyKey("idem-1")
                .status("INITIATED")
                .transactionNo("tx-1")
                .invoiceUrl("http://inv")
                .refundedAmount(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Payment domain = paymentMapper.toDomain(entity);
        assertNotNull(domain);
        assertEquals("pay-1", domain.getPaymentId());

        PaymentEntity entity2 = paymentMapper.toEntity(domain, entity);
        assertNotNull(entity2);
        assertEquals("pay-1", entity2.getPaymentId());
    }

    @Test
    void paymentMethodDomainMapperShouldConvertEntityAndDomain() {
        PaymentMethodEntity entity = PaymentMethodEntity.builder()
                .methodId("pm-1")
                .userId("user-1")
                .provider("STRIPE")
                .gatewayToken("tok-1")
                .status("LINKED")
                .last4Digits("4242")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PaymentMethod domain = methodMapper.toDomain(entity);
        assertNotNull(domain);
        assertEquals("pm-1", domain.getMethodId());

        PaymentMethodEntity entity2 = methodMapper.toEntity(domain, entity);
        assertNotNull(entity2);
        assertEquals("pm-1", entity2.getMethodId());
    }
}
