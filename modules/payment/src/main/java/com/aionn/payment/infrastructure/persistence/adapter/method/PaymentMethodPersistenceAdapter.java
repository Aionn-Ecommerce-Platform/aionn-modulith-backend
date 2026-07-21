package com.aionn.payment.infrastructure.persistence.adapter.method;

import com.aionn.payment.application.port.out.PaymentMethodPersistencePort;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.infrastructure.persistence.entity.PaymentMethodEntity;
import com.aionn.payment.infrastructure.persistence.mapper.PaymentMethodDomainMapper;
import com.aionn.payment.infrastructure.persistence.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentMethodPersistenceAdapter implements PaymentMethodPersistencePort {

    private final PaymentMethodRepository jpa;
    private final PaymentMethodDomainMapper mapper;

    @Override
    public PaymentMethod save(PaymentMethod method) {
        PaymentMethodEntity existing = jpa.findById(method.getMethodId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(method, existing)));
    }

    @Override
    public Optional<PaymentMethod> findById(String methodId) {
        return jpa.findById(methodId).map(mapper::toDomain);
    }

    @Override
    public List<PaymentMethod> findActiveByUserId(String userId) {
        return jpa.findByUserIdAndStatusNot(userId, "REMOVED").stream()
                .map(mapper::toDomain)
                .toList();
    }
}

