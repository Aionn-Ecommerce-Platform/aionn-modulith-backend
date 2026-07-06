package com.aionn.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "catalog_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogSettingsJpaEntity {

    @Id
    @Column(name = "key", nullable = false, length = 100)
    private String key;

    @Column(name = "value", nullable = false, length = 255)
    private String value;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
