package com.aionn.payment.infrastructure.persistence.mapper;

import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.domain.valueobject.PaymentMethodStatus;
import com.aionn.payment.infrastructure.persistence.entity.PaymentMethodEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMethodDomainMapper {

    public PaymentMethod toDomain(PaymentMethodEntity e) {
        return new PaymentMethod(
                e.getMethodId(),
                e.getUserId(),
                e.getProvider(),
                e.getLast4Digits(),
                e.getGatewayToken(),
                PaymentMethodStatus.valueOf(e.getStatus()),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getVerifiedAt());
    }

    public PaymentMethodEntity toEntity(PaymentMethod m, PaymentMethodEntity existing) {
        PaymentMethodEntity entity = existing != null ? existing
                : PaymentMethodEntity.builder()
                        .methodId(m.getMethodId())
                        .userId(m.getUserId())
                        .provider(m.getProvider())
                        .last4Digits(m.getLast4Digits())
                        .gatewayToken(m.getGatewayToken())
                        .build();
        entity.setStatus(m.getStatus().name());
        entity.setVerifiedAt(m.getVerifiedAt());
        return entity;
    }
}

