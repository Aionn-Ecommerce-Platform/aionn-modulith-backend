package com.aionn.payment.infrastructure.persistence.mapper;

import com.aionn.payment.domain.model.TransactionLedger;
import com.aionn.payment.domain.valueobject.LedgerEntryType;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.payment.infrastructure.persistence.entity.TransactionLedgerEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionLedgerDomainMapper {

    public TransactionLedger toDomain(TransactionLedgerEntity e) {
        return new TransactionLedger(
                e.getLedgerId(),
                e.getPaymentId(),
                Money.of(e.getAmount(), e.getCurrency()),
                LedgerEntryType.valueOf(e.getType()),
                e.getGateway(),
                e.getGatewayTransactionNo(),
                e.getOccurredAt());
    }

    public TransactionLedgerEntity toEntity(TransactionLedger l) {
        return TransactionLedgerEntity.builder()
                .ledgerId(l.getLedgerId())
                .paymentId(l.getPaymentId())
                .amount(l.getAmount().amount())
                .currency(l.getAmount().currency())
                .type(l.getType().name())
                .gateway(l.getGateway())
                .gatewayTransactionNo(l.getGatewayTransactionNo())
                .occurredAt(l.getOccurredAt())
                .build();
    }
}

