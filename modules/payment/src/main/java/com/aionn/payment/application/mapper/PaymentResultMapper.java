package com.aionn.payment.application.mapper;

import com.aionn.payment.application.dto.ledger.result.LedgerResult;
import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.domain.model.Payment;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.domain.model.TransactionLedger;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultMapper {

    public PaymentResult toResult(Payment p) {
        return new PaymentResult(
                p.getPaymentId(),
                p.getOrderId(),
                p.getUserId(),
                p.getPaymentMethodId(),
                p.getAmount().amount(),
                p.getRefundedAmount().amount(),
                p.getAmount().currency(),
                p.getGateway().name(),
                p.getStatus().name(),
                p.getTransactionNo(),
                p.getInvoiceUrl(),
                p.getErrorCode(),
                p.getErrorReason(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getPaidAt(),
                p.getFailedAt());
    }

    public PaymentMethodResult toResult(PaymentMethod m) {
        return new PaymentMethodResult(
                m.getMethodId(),
                m.getUserId(),
                m.getProvider(),
                m.getLast4Digits(),
                m.getStatus().name(),
                m.getCreatedAt(),
                m.getUpdatedAt(),
                m.getVerifiedAt());
    }

    public LedgerResult toResult(TransactionLedger l) {
        return new LedgerResult(
                l.getLedgerId(),
                l.getPaymentId(),
                l.getAmount().amount(),
                l.getAmount().currency(),
                l.getType().name(),
                l.getGateway(),
                l.getGatewayTransactionNo(),
                l.getOccurredAt());
    }
}

