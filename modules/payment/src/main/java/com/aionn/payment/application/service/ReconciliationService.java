package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.ledger.result.ReconciliationResult;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.payment.application.port.out.TransactionLedgerPersistencePort;
import com.aionn.payment.domain.event.LedgerEvents;
import com.aionn.payment.domain.model.TransactionLedger;
import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReconciliationService {

    private final TransactionLedgerPersistencePort ledgerRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public ReconciliationResult reconcile(
            String gateway, Instant from, Instant to, Map<String, BigDecimal> gatewayReport) {
        List<TransactionLedger> entries = ledgerRepository.findByGatewayBetween(gateway, from, to);
        int matched = 0;
        List<String> mismatched = new ArrayList<>();
        for (TransactionLedger entry : entries) {
            BigDecimal reportedAmount = gatewayReport.get(entry.getGatewayTransactionNo());
            if (reportedAmount != null && reportedAmount.compareTo(entry.getAmount().amount()) == 0) {
                matched++;
            } else {
                mismatched.add(entry.getGatewayTransactionNo());
            }
        }
        Instant now = clock.instant();
        String reconciliationId = "rec-" + IdGenerator.ulid();

        LedgerEvents.PaymentReconciled event = new LedgerEvents.PaymentReconciled(
                reconciliationId, gateway, matched, mismatched.size(), now, now);

        EventEnvelope envelope = new EventEnvelope(
                IdGenerator.ulid(),
                "Reconciliation",
                reconciliationId,
                event,
                event.occurredAt());

        eventPublisher.publish(envelope);

        log.info("Reconciliation [{}] gateway={} matched={} mismatched={}", reconciliationId, gateway,
                matched, mismatched.size());
        return new ReconciliationResult(reconciliationId, gateway, matched, mismatched.size(), mismatched, now);
    }
}
