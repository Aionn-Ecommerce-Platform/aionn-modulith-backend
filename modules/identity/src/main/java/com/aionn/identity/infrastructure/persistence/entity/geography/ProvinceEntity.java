package com.aionn.identity.infrastructure.persistence.entity.geography;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "provinces", indexes = {
        @Index(name = "idx_province_country", columnList = "country_code")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceEntity {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false)
    private Boolean active = true;
}
