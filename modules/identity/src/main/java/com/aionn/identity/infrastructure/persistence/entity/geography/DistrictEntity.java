package com.aionn.identity.infrastructure.persistence.entity.geography;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "districts", indexes = {
        @Index(name = "idx_district_province", columnList = "province_code")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DistrictEntity {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "province_code", nullable = false, length = 10, insertable = false, updatable = false)
    private String provinceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_code", nullable = false)
    private ProvinceEntity province;

    @Column(nullable = false)
    private Boolean active = true;
}
