package com.aionn.payment.infrastructure.persistence.repository;

import com.aionn.payment.infrastructure.persistence.entity.TransactionLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TransactionLedgerRepository extends JpaRepository<TransactionLedgerEntity, String> {

    List<TransactionLedgerEntity> findByPaymentId(String paymentId);

    List<TransactionLedgerEntity> findByGatewayAndOccurredAtBetween(String gateway, Instant from, Instant to);
}

