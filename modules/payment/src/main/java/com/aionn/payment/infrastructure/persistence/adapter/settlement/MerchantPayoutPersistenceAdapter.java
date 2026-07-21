package com.aionn.payment.infrastructure.persistence.adapter.settlement;

import com.aionn.payment.application.port.out.MerchantPayoutPersistencePort;
import com.aionn.payment.domain.model.MerchantPayout;
import com.aionn.payment.domain.valueobject.PayoutStatus;
import com.aionn.payment.infrastructure.persistence.entity.MerchantPayoutEntity;
import com.aionn.payment.infrastructure.persistence.repository.MerchantPayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MerchantPayoutPersistenceAdapter implements MerchantPayoutPersistencePort {

    private final MerchantPayoutRepository jpa;

    @Override
    public MerchantPayout save(MerchantPayout payout) {
        return toDomain(jpa.save(toEntity(payout)));
    }

    @Override
    public Optional<MerchantPayout> findById(String payoutId) {
        return jpa.findById(payoutId).map(this::toDomain);
    }

    @Override
    public List<MerchantPayout> findByMerchant(String merchantId, int limit) {
        return jpa.findByMerchantIdOrderByRequestedAtDesc(merchantId,
                PageRequest.of(0, Math.max(1, limit))).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public List<MerchantPayout> findByStatus(PayoutStatus status, int limit) {
        return jpa.findByStatusOrderByRequestedAtAsc(status.name(),
                PageRequest.of(0, Math.max(1, limit))).stream()
                .map(this::toDomain).toList();
    }

    private MerchantPayoutEntity toEntity(MerchantPayout p) {
        return MerchantPayoutEntity.builder()
                .payoutId(p.getPayoutId())
                .merchantId(p.getMerchantId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus().name())
                .bankName(p.getBankName())
                .bankAccountNo(p.getBankAccountNo())
                .bankAccountName(p.getBankAccountName())
                .externalRef(p.getExternalRef())
                .note(p.getNote())
                .requestedAt(p.getRequestedAt())
                .completedAt(p.getCompletedAt())
                .failedAt(p.getFailedAt())
                .failureReason(p.getFailureReason())
                .version(p.getVersion())
                .build();
    }

    private MerchantPayout toDomain(MerchantPayoutEntity e) {
        return new MerchantPayout(e.getPayoutId(), e.getMerchantId(), e.getAmount(),
                e.getCurrency(), PayoutStatus.valueOf(e.getStatus()),
                e.getBankName(), e.getBankAccountNo(), e.getBankAccountName(),
                e.getExternalRef(), e.getNote(), e.getRequestedAt(),
                e.getCompletedAt(), e.getFailedAt(), e.getFailureReason(),
                e.getVersion());
    }
}
