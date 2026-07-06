package com.aionn.catalog.infrastructure.persistence.adapter.settings;

import com.aionn.catalog.infrastructure.persistence.entity.CatalogSettingsJpaEntity;
import com.aionn.catalog.infrastructure.persistence.repository.settings.CatalogSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogSettingsPersistenceAdapterTest {

    @Mock
    private CatalogSettingsRepository repository;

    @InjectMocks
    private CatalogSettingsPersistenceAdapter adapter;

    @Test
    void getDefaultCommissionRateReturnsPersistedValue() {
        CatalogSettingsJpaEntity entity = CatalogSettingsJpaEntity.builder()
                .key("default_commission_rate")
                .value("0.0750")
                .build();
        when(repository.findById("default_commission_rate")).thenReturn(Optional.of(entity));

        BigDecimal result = adapter.getDefaultCommissionRate();

        assertThat(result).isEqualByComparingTo("0.0750");
    }

    @Test
    void getDefaultCommissionRateReturnsFallbackWhenAbsent() {
        when(repository.findById("default_commission_rate")).thenReturn(Optional.empty());

        BigDecimal result = adapter.getDefaultCommissionRate();

        assertThat(result).isEqualByComparingTo("0.0500");
    }

    @Test
    void updateDefaultCommissionRateCreatesEntityWhenAbsent() {
        when(repository.findById("default_commission_rate")).thenReturn(Optional.empty());

        adapter.updateDefaultCommissionRate(new BigDecimal("0.0800"));

        ArgumentCaptor<CatalogSettingsJpaEntity> captor = ArgumentCaptor.forClass(CatalogSettingsJpaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getKey()).isEqualTo("default_commission_rate");
        assertThat(captor.getValue().getValue()).isEqualTo("0.0800");
    }

    @Test
    void updateDefaultCommissionRateUpdatesExistingEntity() {
        CatalogSettingsJpaEntity existing = CatalogSettingsJpaEntity.builder()
                .key("default_commission_rate")
                .value("0.0500")
                .build();
        when(repository.findById("default_commission_rate")).thenReturn(Optional.of(existing));

        adapter.updateDefaultCommissionRate(new BigDecimal("0.0600"));

        verify(repository).save(any(CatalogSettingsJpaEntity.class));
        assertThat(existing.getValue()).isEqualTo("0.0600");
    }
}
