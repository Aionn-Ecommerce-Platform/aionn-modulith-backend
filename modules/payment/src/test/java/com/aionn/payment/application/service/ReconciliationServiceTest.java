package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.ledger.result.ReconciliationResult;
import com.aionn.payment.application.port.out.TransactionLedgerPersistencePort;
import com.aionn.payment.domain.model.TransactionLedger;
import com.aionn.payment.domain.valueobject.LedgerEntryType;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private TransactionLedgerPersistencePort ledgerRepository;
    @Mock
    private EventPublisher eventPublisher;

    private final Instant fixedInstant = Instant.parse("2026-01-01T00:00:00Z");
    private final Clock clock = Clock.fixed(fixedInstant, java.time.ZoneOffset.UTC);

    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService(ledgerRepository, eventPublisher, clock);
    }

    @Test
    void reconcileMatchesAndMismatchesEntries() {
        TransactionLedger matched = TransactionLedger.record("l1", "p1",
                Money.of(new BigDecimal("100"), "VND"), LedgerEntryType.CREDIT, "STRIPE", "txn-1", fixedInstant);
        TransactionLedger mismatched = TransactionLedger.record("l2", "p2",
                Money.of(new BigDecimal("200"), "VND"), LedgerEntryType.CREDIT, "STRIPE", "txn-2", fixedInstant);
        TransactionLedger missing = TransactionLedger.record("l3", "p3",
                Money.of(new BigDecimal("50"), "VND"), LedgerEntryType.CREDIT, "STRIPE", "txn-3", fixedInstant);

        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2024-01-02T00:00:00Z");
        when(ledgerRepository.findByGatewayBetween("STRIPE", from, to))
                .thenReturn(List.of(matched, mismatched, missing));

        Map<String, BigDecimal> report = Map.of(
                "txn-1", new BigDecimal("100"),
                "txn-2", new BigDecimal("999"));

        ReconciliationResult result = service.reconcile("STRIPE", from, to, report);

        assertThat(result.gateway()).isEqualTo("STRIPE");
        assertThat(result.matchedCount()).isEqualTo(1);
        assertThat(result.mismatchedCount()).isEqualTo(2);
        assertThat(result.mismatchedTransactionIds()).containsExactlyInAnyOrder("txn-2", "txn-3");
        assertThat(result.reconciliationId()).startsWith("rec-");

        ArgumentCaptor<EventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(eventPublisher).publish(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().aggregateType()).isEqualTo("Reconciliation");
    }

    @Test
    void reconcileEmptyReportMarksEverythingMismatched() {
        TransactionLedger entry = TransactionLedger.record("l1", "p1",
                Money.of(new BigDecimal("75"), "VND"), LedgerEntryType.CREDIT, "VNPAY", "vn-1", fixedInstant);
        Instant from = fixedInstant.minusSeconds(60);
        Instant to = fixedInstant;
        when(ledgerRepository.findByGatewayBetween(eq("VNPAY"), any(), any()))
                .thenReturn(List.of(entry));

        ReconciliationResult result = service.reconcile("VNPAY", from, to, Map.of());

        assertThat(result.matchedCount()).isZero();
        assertThat(result.mismatchedCount()).isEqualTo(1);
        assertThat(result.mismatchedTransactionIds()).containsExactly("vn-1");
    }
}
