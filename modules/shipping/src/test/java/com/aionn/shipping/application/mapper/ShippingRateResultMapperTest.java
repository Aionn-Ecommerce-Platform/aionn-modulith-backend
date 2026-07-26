package com.aionn.shipping.application.mapper;

import com.aionn.shipping.application.dto.rate.result.ShippingRateResult;
import com.aionn.shipping.domain.model.ShippingRate;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ShippingRateResultMapperTest {

    private final ShippingRateResultMapper mapper = Mappers.getMapper(ShippingRateResultMapper.class);

    @Test
    void mapsAllFields() {
        ShippingRate r = ShippingRate.configure("r1", "HCM", new BigDecimal("30000"), "VND", "<=2kg");

        ShippingRateResult result = mapper.toResult(r);

        assertThat(result.rateId()).isEqualTo("r1");
        assertThat(result.zoneCode()).isEqualTo("HCM");
        assertThat(result.baseFee()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(result.currency()).isEqualTo("VND");
        assertThat(result.condition()).isEqualTo("<=2kg");
    }
}
