package com.aionn.payment.application.port.out;

import com.aionn.payment.domain.model.TransactionLedger;

import java.time.Instant;
import java.util.List;

public interface TransactionLedgerPersistencePort {

    TransactionLedger save(TransactionLedger entry);

    List<TransactionLedger> findByPaymentId(String paymentId);

    List<TransactionLedger> findByGatewayBetween(String gateway, Instant from, Instant to);
}

