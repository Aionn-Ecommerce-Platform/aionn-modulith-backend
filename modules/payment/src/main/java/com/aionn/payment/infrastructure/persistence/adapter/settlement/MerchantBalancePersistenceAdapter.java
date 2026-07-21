package com.aionn.payment.infrastructure.persistence.adapter.settlement;

import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.infrastructure.persistence.entity.MerchantBalanceEntity;
import com.aionn.payment.infrastructure.persistence.repository.MerchantBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MerchantBalancePersistenceAdapter implements MerchantBalancePersistencePort {

    private final MerchantBalanceRepository jpa;

    @Override
    public MerchantBalance save(MerchantBalance balance) {
        MerchantBalanceEntity entity = jpa
                .findByMerchantAndCurrency(balance.getMerchantId(), balance.getCurrency())
                .orElseGet(() -> MerchantBalanceEntity.builder()
                        .merchantId(balance.getMerchantId())
                        .currency(balance.getCurrency())
                        .createdAt(balance.getCreatedAt())
                        .build());
        entity.setPending(balance.getPending());
        entity.setAvailable(balance.getAvailable());
        entity.setUpdatedAt(Instant.now());
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<MerchantBalance> find(String merchantId, String currency) {
        return jpa.findByMerchantAndCurrency(merchantId, currency).map(this::toDomain);
    }

    @Override
    public Optional<MerchantBalance> lockForUpdate(String merchantId, String currency) {
        return jpa.lockByMerchantAndCurrency(merchantId, currency).map(this::toDomain);
    }

    private MerchantBalance toDomain(MerchantBalanceEntity e) {
        return new MerchantBalance(e.getMerchantId(), e.getCurrency(),
                e.getPending(), e.getAvailable(), e.getVersion(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
