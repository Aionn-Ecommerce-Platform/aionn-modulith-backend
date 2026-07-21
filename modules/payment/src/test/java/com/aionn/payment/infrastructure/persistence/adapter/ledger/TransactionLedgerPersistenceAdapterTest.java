package com.aionn.payment.infrastructure.persistence.adapter.ledger;

import com.aionn.payment.domain.model.TransactionLedger;
import com.aionn.payment.infrastructure.persistence.entity.TransactionLedgerEntity;
import com.aionn.payment.infrastructure.persistence.mapper.TransactionLedgerDomainMapper;
import com.aionn.payment.infrastructure.persistence.repository.TransactionLedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionLedgerPersistenceAdapterTest {

    @Mock
    private TransactionLedgerRepository jpa;
    @Mock
    private TransactionLedgerDomainMapper mapper;

    @InjectMocks
    private TransactionLedgerPersistenceAdapter adapter;

    @Test
    void shouldSaveAndFindByPaymentId() {
        TransactionLedger entry = TransactionLedger.record("entry-1", "pay-1",
                com.aionn.sharedkernel.domain.vo.Money.of(BigDecimal.TEN, "VND"),
                com.aionn.payment.domain.valueobject.LedgerEntryType.CREDIT,
                "STRIPE", "tx-1", Instant.now());

        TransactionLedgerEntity entity = TransactionLedgerEntity.builder().ledgerId("entry-1").paymentId("pay-1").build();

        when(mapper.toEntity(any())).thenReturn(entity);
        when(jpa.save(any())).thenReturn(entity);
        when(mapper.toDomain(any())).thenReturn(entry);
        when(jpa.findByPaymentId("pay-1")).thenReturn(List.of(entity));

        TransactionLedger saved = adapter.save(entry);
        assertNotNull(saved);

        List<TransactionLedger> list = adapter.findByPaymentId("pay-1");
        assertEquals(1, list.size());
    }
}
