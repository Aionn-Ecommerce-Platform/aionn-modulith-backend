package com.aionn.promotion.infrastructure.persistence.adapter.voucher;

import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.promotion.infrastructure.persistence.entity.VoucherEntity;
import com.aionn.promotion.infrastructure.persistence.mapper.VoucherDomainMapper;
import com.aionn.promotion.infrastructure.persistence.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;

@Repository
@RequiredArgsConstructor
public class VoucherPersistenceAdapter implements VoucherPersistencePort {

    private final VoucherRepository jpa;
    private final VoucherDomainMapper mapper;

    @Override
    public Voucher save(Voucher voucher) {
        VoucherEntity existing = jpa.findById(voucher.getVoucherCode()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(voucher, existing)));
    }

    @Override
    public Optional<Voucher> findByCode(String voucherCode) {
        return jpa.findById(voucherCode).map(mapper::toDomain);
    }

    @Override
    public Optional<Voucher> lockByCode(String voucherCode) {
        return jpa.findForUpdate(voucherCode).map(mapper::toDomain);
    }

    @Override
    public List<Voucher> findByCampaignId(String campaignId, int limit) {
        return jpa.findByCampaignId(campaignId, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Voucher> findByMerchantId(String merchantId, int limit) {
        return jpa.findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

