package com.aionn.payment.infrastructure.persistence.adapter.payment;

import com.aionn.payment.application.port.out.RefundOperationPersistencePort;
import com.aionn.payment.domain.model.RefundOperation;
import com.aionn.payment.infrastructure.persistence.entity.RefundOperationEntity;
import com.aionn.payment.infrastructure.persistence.repository.RefundOperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefundOperationPersistenceAdapter implements RefundOperationPersistencePort {

    private final RefundOperationRepository repository;

    @Override
    public Optional<RefundOperation> findByIdempotencyKey(String idempotencyKey) {
        return repository.findById(idempotencyKey).map(this::toDomain);
    }

    @Override
    public BigDecimal sumReservedAmount(String paymentId) {
        return repository.sumReservedAmount(paymentId);
    }

    @Override
    public RefundOperation save(RefundOperation operation) {
        return toDomain(repository.save(toEntity(operation)));
    }

    private RefundOperation toDomain(RefundOperationEntity entity) {
        return new RefundOperation(entity.getIdempotencyKey(), entity.getPaymentId(), entity.getAmount(),
                entity.getCurrency(), entity.getReason(), RefundOperation.Status.valueOf(entity.getStatus()),
                entity.getProviderRefundId(), entity.getFailureReason(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private RefundOperationEntity toEntity(RefundOperation operation) {
        return RefundOperationEntity.builder()
                .idempotencyKey(operation.idempotencyKey())
                .paymentId(operation.paymentId())
                .amount(operation.amount())
                .currency(operation.currency())
                .reason(operation.reason())
                .status(operation.status().name())
                .providerRefundId(operation.providerRefundId())
                .failureReason(operation.failureReason())
                .createdAt(operation.createdAt())
                .updatedAt(operation.updatedAt())
                .build();
    }
}
