package com.aionn.payment.infrastructure.persistence.adapter.settlement;

import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.infrastructure.persistence.entity.MerchantBalanceEntity;
import com.aionn.payment.infrastructure.persistence.repository.MerchantBalanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantBalancePersistenceAdapterTest {

    @Mock
    private MerchantBalanceRepository jpa;

    private MerchantBalancePersistenceAdapter adapter;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        adapter = new MerchantBalancePersistenceAdapter(jpa, Clock.systemUTC());
    }

    @Test
    void shouldFindAndSaveMerchantBalance() {
        MerchantBalanceEntity entity = MerchantBalanceEntity.builder()
                .merchantId("merch-1")
                .currency("VND")
                .pending(BigDecimal.ZERO)
                .available(BigDecimal.valueOf(100000))
                .receivable(BigDecimal.ZERO)
                .build();

        when(jpa.findByMerchantAndCurrency("merch-1", "VND")).thenReturn(Optional.of(entity));
        when(jpa.save(any())).thenReturn(entity);

        Optional<MerchantBalance> result = adapter.find("merch-1", "VND");
        assertTrue(result.isPresent());

        MerchantBalance saved = adapter.save(result.get());
        assertNotNull(saved);
        assertEquals("merch-1", saved.getMerchantId());
    }

    @Test
    void createIfAbsentThenLocksTheWinningBalance() {
        Instant now = Instant.parse("2026-08-05T00:00:00Z");
        MerchantBalanceEntity entity = MerchantBalanceEntity.builder()
                .merchantId("merch-1")
                .currency("VND")
                .pending(BigDecimal.ZERO)
                .available(BigDecimal.ZERO)
                .receivable(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();
        when(jpa.lockByMerchantAndCurrency("merch-1", "VND")).thenReturn(Optional.of(entity));

        MerchantBalance result = adapter.createIfAbsentAndLock("merch-1", "VND", now);

        assertEquals("merch-1", result.getMerchantId());
        verify(jpa).insertIfAbsent("merch-1", "VND", now);
        verify(jpa).lockByMerchantAndCurrency("merch-1", "VND");
    }

    @Test
    void exposesLedgerBalanceMismatches() {
        MerchantBalanceRepository.ReconciliationMismatchProjection row =
                mock(MerchantBalanceRepository.ReconciliationMismatchProjection.class);
        when(row.getMerchantId()).thenReturn("merch-1");
        when(row.getCurrency()).thenReturn("VND");
        when(row.getActualPending()).thenReturn(BigDecimal.TEN);
        when(row.getLedgerPending()).thenReturn(BigDecimal.ONE);
        when(row.getActualAvailable()).thenReturn(BigDecimal.ZERO);
        when(row.getLedgerAvailable()).thenReturn(BigDecimal.ZERO);
        when(row.getActualReceivable()).thenReturn(BigDecimal.ZERO);
        when(row.getLedgerReceivable()).thenReturn(BigDecimal.ZERO);
        when(jpa.count()).thenReturn(1L);
        when(jpa.findReconciliationMismatches()).thenReturn(java.util.List.of(row));

        assertEquals(1L, adapter.countBalances());
        var mismatches = adapter.findReconciliationMismatches();

        assertEquals(1, mismatches.size());
        assertEquals("merch-1", mismatches.get(0).merchantId());
        assertEquals(BigDecimal.ONE, mismatches.get(0).ledgerPending());
    }
}
