package com.aionn.identity.infrastructure.persistence.repository.geography;

import com.aionn.identity.infrastructure.persistence.entity.geography.DistrictEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<DistrictEntity, String> {

    List<DistrictEntity> findByProvinceCodeAndActiveTrue(String provinceCode);

    Optional<DistrictEntity> findByCodeAndActiveTrue(String code);
}
