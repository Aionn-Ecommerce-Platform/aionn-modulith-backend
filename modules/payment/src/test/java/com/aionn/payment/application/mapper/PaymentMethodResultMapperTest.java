package com.aionn.payment.application.mapper;

import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.domain.model.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodResultMapperTest {

    private final PaymentMethodResultMapper mapper = Mappers.getMapper(PaymentMethodResultMapper.class);

    @Test
    void mapsLinkedMethod() {
        PaymentMethod m = PaymentMethod.link("m1", "u1", "stripe", "4242", "tok-abc", java.time.Instant.now());

        PaymentMethodResult result = mapper.toResult(m);

        assertThat(result.methodId()).isEqualTo("m1");
        assertThat(result.userId()).isEqualTo("u1");
        assertThat(result.status()).isEqualTo("LINKED");
        assertThat(result.last4Digits()).isEqualTo("4242");
    }
}
