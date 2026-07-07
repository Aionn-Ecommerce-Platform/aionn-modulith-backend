package com.aionn.catalog.infrastructure.persistence.adapter.settings;

import com.aionn.catalog.application.port.out.settings.CatalogSettingsPort;
import com.aionn.catalog.infrastructure.persistence.entity.CatalogSettingsJpaEntity;
import com.aionn.catalog.infrastructure.persistence.repository.settings.CatalogSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class CatalogSettingsPersistenceAdapter implements CatalogSettingsPort {

    private final CatalogSettingsRepository repository;

    private static final String DEFAULT_COMMISSION_RATE_KEY = "default_commission_rate";
    private static final String FALLBACK_RATE = "0.0500";

    @Override
    public BigDecimal getDefaultCommissionRate() {
        return repository.findById(DEFAULT_COMMISSION_RATE_KEY)
                .map(CatalogSettingsJpaEntity::getValue)
                .map(BigDecimal::new)
                .orElse(new BigDecimal(FALLBACK_RATE));
    }

    @Override
    public void updateDefaultCommissionRate(BigDecimal rate) {
        CatalogSettingsJpaEntity entity = repository.findById(DEFAULT_COMMISSION_RATE_KEY)
                .orElseGet(() -> CatalogSettingsJpaEntity.builder().key(DEFAULT_COMMISSION_RATE_KEY).build());
        entity.setValue(rate.toString());
        repository.save(entity);
    }
}
