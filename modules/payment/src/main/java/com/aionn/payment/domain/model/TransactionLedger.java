package com.aionn.payment.domain.model;

import com.aionn.sharedkernel.domain.model.AggregateRoot;
import com.aionn.payment.domain.event.LedgerEvents;
import com.aionn.payment.domain.valueobject.LedgerEntryType;
import com.aionn.sharedkernel.domain.vo.Money;
import lombok.Getter;

import java.time.Instant;

@Getter
public class TransactionLedger extends AggregateRoot {

    private final String ledgerId;
    private final String paymentId;
    private final Money amount;
    private final LedgerEntryType type;
    private final String gateway;
    private final String gatewayTransactionNo;
    private final Instant occurredAt;

    public TransactionLedger(
            String ledgerId,
            String paymentId,
            Money amount,
            LedgerEntryType type,
            String gateway,
            String gatewayTransactionNo,
            Instant occurredAt) {
        this.ledgerId = ledgerId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.type = type;
        this.gateway = gateway;
        this.gatewayTransactionNo = gatewayTransactionNo;
        this.occurredAt = occurredAt;
    }

    public static TransactionLedger record(
            String ledgerId,
            String paymentId,
            Money amount,
            LedgerEntryType type,
            String gateway,
            String gatewayTransactionNo,
            Instant now) {
        TransactionLedger l = new TransactionLedger(ledgerId, paymentId, amount, type, gateway,
                gatewayTransactionNo, now);
        l.registerEvent(new LedgerEvents.LedgerEntryRecorded(
                ledgerId, paymentId, amount.amount(), amount.currency(), type, gateway, now));
        return l;
    }

    @Override
    protected String aggregateId() {
        return ledgerId;
    }
}
