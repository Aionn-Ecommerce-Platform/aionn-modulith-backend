package com.aionn.payment.infrastructure.persistence.adapter.ledger;

import com.aionn.payment.application.port.out.TransactionLedgerPersistencePort;
import com.aionn.payment.domain.model.TransactionLedger;
import com.aionn.payment.infrastructure.persistence.mapper.TransactionLedgerDomainMapper;
import com.aionn.payment.infrastructure.persistence.repository.TransactionLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TransactionLedgerPersistenceAdapter implements TransactionLedgerPersistencePort {

    private final TransactionLedgerRepository jpa;
    private final TransactionLedgerDomainMapper mapper;

    @Override
    public TransactionLedger save(TransactionLedger entry) {
        return mapper.toDomain(jpa.save(mapper.toEntity(entry)));
    }

    @Override
    public List<TransactionLedger> findByPaymentId(String paymentId) {
        return jpa.findByPaymentId(paymentId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<TransactionLedger> findByGatewayBetween(String gateway, Instant from, Instant to) {
        return jpa.findByGatewayAndOccurredAtBetween(gateway, from, to).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

