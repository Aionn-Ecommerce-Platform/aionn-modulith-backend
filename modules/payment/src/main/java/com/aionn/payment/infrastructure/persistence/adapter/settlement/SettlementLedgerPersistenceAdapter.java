package com.aionn.payment.infrastructure.persistence.adapter.settlement;

import com.aionn.payment.application.port.out.SettlementLedgerPersistencePort;
import com.aionn.payment.domain.model.SettlementLedgerEntry;
import com.aionn.payment.infrastructure.persistence.entity.SettlementLedgerEntity;
import com.aionn.payment.infrastructure.persistence.repository.SettlementLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SettlementLedgerPersistenceAdapter implements SettlementLedgerPersistencePort {

    private final SettlementLedgerRepository jpa;

    @Override
    public SettlementLedgerEntry save(SettlementLedgerEntry entry) {
        SettlementLedgerEntity e = SettlementLedgerEntity.builder()
                .entryId(entry.getEntryId())
                .merchantId(entry.getMerchantId())
                .orderId(entry.getOrderId())
                .paymentId(entry.getPaymentId())
                .payoutId(entry.getPayoutId())
                .kind(entry.getKind().name())
                .gross(entry.getGross())
                .commission(entry.getCommission())
                .net(entry.getNet())
                .currency(entry.getCurrency())
                .note(entry.getNote())
                .createdAt(entry.getCreatedAt())
                .build();
        return toDomain(jpa.save(e));
    }

    @Override
    public List<SettlementLedgerEntry> findByMerchant(String merchantId, int limit) {
        return jpa.findByMerchantIdOrderByCreatedAtDesc(merchantId,
                PageRequest.of(0, Math.max(1, limit))).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public List<SettlementLedgerEntry> findByOrder(String orderId) {
        return jpa.findByOrderId(orderId).stream().map(this::toDomain).toList();
    }

    private SettlementLedgerEntry toDomain(SettlementLedgerEntity e) {
        return new SettlementLedgerEntry(e.getEntryId(), e.getMerchantId(),
                e.getOrderId(), e.getPaymentId(), e.getPayoutId(),
                SettlementLedgerEntry.SettlementKind.valueOf(e.getKind()),
                e.getGross(), e.getCommission(), e.getNet(),
                e.getCurrency(), e.getNote(), e.getCreatedAt());
    }
}
