package com.aionn.ordering.application.mapper;

import com.aionn.ordering.application.dto.returns.result.ReturnResult;
import com.aionn.ordering.domain.model.OrderReturn;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnResultMapperTest {

    private final ReturnResultMapper mapper = Mappers.getMapper(ReturnResultMapper.class);

    @Test
    void mapsRefundCurrencyFromMoney() {
        OrderReturn r = OrderReturn.request("ret-1", "ord-1", "u", "m", "broken", "img://1");
        r.approve(Money.of(new BigDecimal("50000"), "VND"), "wh-1");

        ReturnResult result = mapper.toResult(r);

        assertThat(result.returnId()).isEqualTo("ret-1");
        assertThat(result.refundAmount()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(result.currency()).isEqualTo("VND");
        assertThat(result.status()).isEqualTo("APPROVED");
    }

    @Test
    void mapsNullRefundWhenNotPresent() {
        OrderReturn r = OrderReturn.request("ret-1", "ord-1", "u", "m", "broken", null);

        ReturnResult result = mapper.toResult(r);

        assertThat(result.refundAmount()).isNull();
        assertThat(result.currency()).isNull();
    }
}
