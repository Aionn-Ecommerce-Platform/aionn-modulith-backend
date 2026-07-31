package com.aionn.identity.infrastructure.persistence.repository.geography;

import com.aionn.identity.infrastructure.persistence.entity.geography.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends JpaRepository<CountryEntity, String> {

    List<CountryEntity> findByActiveTrue();

    Optional<CountryEntity> findByCodeAndActiveTrue(String code);
}
