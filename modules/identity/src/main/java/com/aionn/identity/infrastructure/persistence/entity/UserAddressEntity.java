package com.aionn.identity.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_addresses", indexes = {
        @Index(name = "idx_user_addresses_user_id", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserAddressEntity {

    @Id
    @Column(name = "address_id", nullable = false, length = 26)
    private String addressId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "full_address", columnDefinition = "TEXT", nullable = false)
    private String fullAddress;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "province_code", nullable = false, length = 20)
    private String provinceCode;

    @Column(name = "province_name", length = 255)
    private String provinceName;

    @Column(name = "district_code", nullable = false, length = 20)
    private String districtCode;

    @Column(name = "district_name", length = 255)
    private String districtName;

    @Column(name = "ward_code", nullable = false, length = 20)
    private String wardCode;

    @Column(name = "ward_name", length = 255)
    private String wardName;

    @Column(name = "detail_address", nullable = false, length = 500)
    private String detailAddress;

    @Column(name = "address_type", nullable = false, length = 20)
    private String type;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}



