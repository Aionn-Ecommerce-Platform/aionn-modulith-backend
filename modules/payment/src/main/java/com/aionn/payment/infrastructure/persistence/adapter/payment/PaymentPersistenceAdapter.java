package com.aionn.payment.infrastructure.persistence.adapter.payment;

import com.aionn.payment.application.port.out.PaymentPersistencePort;
import com.aionn.payment.domain.model.Payment;
import com.aionn.payment.infrastructure.persistence.entity.PaymentEntity;
import com.aionn.payment.infrastructure.persistence.mapper.PaymentDomainMapper;
import com.aionn.payment.infrastructure.persistence.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentPersistenceAdapter implements PaymentPersistencePort {

    private final PaymentRepository jpa;
    private final PaymentDomainMapper mapper;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity existing = jpa.findById(payment.getPaymentId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(payment, existing)));
    }

    @Override
    public Optional<Payment> findById(String paymentId) {
        return jpa.findById(paymentId).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpa.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findByOrderId(String orderId) {
        return jpa.findByOrderId(orderId).stream().map(mapper::toDomain).toList();
    }
}

