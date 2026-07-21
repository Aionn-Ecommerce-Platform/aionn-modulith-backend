package com.aionn.payment.application.mapper;

import com.aionn.payment.application.dto.ledger.result.LedgerResult;
import com.aionn.payment.domain.model.TransactionLedger;
import com.aionn.payment.domain.valueobject.LedgerEntryType;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerResultMapperTest {

    private final LedgerResultMapper mapper = Mappers.getMapper(LedgerResultMapper.class);

    @Test
    void mapsCreditEntry() {
        TransactionLedger l = TransactionLedger.record("l1", "p1",
                Money.of(new BigDecimal("100"), "VND"), LedgerEntryType.CREDIT, "MOCK", "txn-1", java.time.Instant.now());

        LedgerResult result = mapper.toResult(l);

        assertThat(result.ledgerId()).isEqualTo("l1");
        assertThat(result.paymentId()).isEqualTo("p1");
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(result.currency()).isEqualTo("VND");
        assertThat(result.type()).isEqualTo("CREDIT");
        assertThat(result.gateway()).isEqualTo("MOCK");
    }
}
