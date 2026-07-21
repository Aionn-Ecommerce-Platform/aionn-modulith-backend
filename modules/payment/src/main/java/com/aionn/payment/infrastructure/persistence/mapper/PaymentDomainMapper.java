package com.aionn.payment.infrastructure.persistence.mapper;

import com.aionn.payment.domain.model.Payment;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.domain.valueobject.PaymentStatus;
import com.aionn.payment.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentDomainMapper {

    public Payment toDomain(PaymentEntity e) {
        return new Payment(
                e.getPaymentId(),
                e.getOrderId(),
                e.getUserId(),
                e.getPaymentMethodId(),
                Money.of(e.getAmount(), e.getCurrency()),
                PaymentGatewayKind.valueOf(e.getGateway()),
                e.getIdempotencyKey(),
                PaymentStatus.valueOf(e.getStatus()),
                e.getTransactionNo(),
                e.getInvoiceUrl(),
                e.getErrorCode(),
                e.getErrorReason(),
                Money.of(e.getRefundedAmount(), e.getCurrency()),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getPaidAt(),
                e.getFailedAt());
    }

    public PaymentEntity toEntity(Payment p, PaymentEntity existing) {
        PaymentEntity entity = existing != null ? existing
                : PaymentEntity.builder()
                        .paymentId(p.getPaymentId())
                        .orderId(p.getOrderId())
                        .userId(p.getUserId())
                        .paymentMethodId(p.getPaymentMethodId())
                        .amount(p.getAmount().amount())
                        .currency(p.getAmount().currency())
                        .gateway(p.getGateway().name())
                        .idempotencyKey(p.getIdempotencyKey())
                        .build();
        entity.setRefundedAmount(p.getRefundedAmount().amount());
        entity.setStatus(p.getStatus().name());
        entity.setTransactionNo(p.getTransactionNo());
        entity.setInvoiceUrl(p.getInvoiceUrl());
        entity.setErrorCode(p.getErrorCode());
        entity.setErrorReason(p.getErrorReason());
        entity.setPaidAt(p.getPaidAt());
        entity.setFailedAt(p.getFailedAt());
        return entity;
    }
}

