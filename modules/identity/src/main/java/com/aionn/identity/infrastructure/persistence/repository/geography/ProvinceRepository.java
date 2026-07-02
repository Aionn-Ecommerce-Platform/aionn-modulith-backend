package com.aionn.identity.infrastructure.persistence.repository.geography;

import com.aionn.identity.infrastructure.persistence.entity.geography.ProvinceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProvinceRepository extends JpaRepository<ProvinceEntity, String> {

    List<ProvinceEntity> findByCountryCodeAndActiveTrue(String countryCode);

    List<ProvinceEntity> findByActiveTrue();

    Optional<ProvinceEntity> findByCodeAndActiveTrue(String code);
}
