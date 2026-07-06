package com.aionn.catalog.infrastructure.persistence.adapter.merchant;

import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.infrastructure.persistence.mapper.MerchantDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.merchant.MerchantRepository;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MerchantPersistenceAdapter implements MerchantPersistencePort {

    private final MerchantRepository jpa;
    private final MerchantDomainMapper mapper;

    @Override
    public Merchant save(Merchant merchant) {
        var saved = jpa.save(mapper.toEntity(merchant));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Merchant> findById(String merchantId) {
        return jpa.findById(merchantId).map(mapper::toDomain);
    }

    @Override
    public Optional<Merchant> findByOwnerId(String ownerId) {
        return jpa.findByOwnerId(ownerId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByOwnerId(String ownerId) {
        return jpa.existsByOwnerId(ownerId);
    }

    @Override
    public List<Merchant> list(OffsetPagination pagination) {
        return jpa.findAll(PageRequest.of(pagination.page(), pagination.size())).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

