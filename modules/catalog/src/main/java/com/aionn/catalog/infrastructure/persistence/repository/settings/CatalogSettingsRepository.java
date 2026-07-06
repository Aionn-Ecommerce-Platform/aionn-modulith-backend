package com.aionn.catalog.infrastructure.persistence.repository.settings;

import com.aionn.catalog.infrastructure.persistence.entity.CatalogSettingsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogSettingsRepository extends JpaRepository<CatalogSettingsJpaEntity, String> {
}
