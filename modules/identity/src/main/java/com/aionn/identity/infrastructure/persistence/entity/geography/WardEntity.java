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
@Table(name = "wards", indexes = {
        @Index(name = "idx_ward_district", columnList = "district_code")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WardEntity {

    @Id
    @Column(length = 15)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "district_code", nullable = false, length = 10, insertable = false, updatable = false)
    private String districtCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_code", nullable = false)
    private DistrictEntity district;

    @Column(nullable = false)
    private Boolean active = true;
}
