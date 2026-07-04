package com.aionn.identity.infrastructure.persistence.repository.geography;

import com.aionn.identity.infrastructure.persistence.entity.geography.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<CountryEntity, String> {

    List<CountryEntity> findByActiveTrue();

    Optional<CountryEntity> findByCodeAndActiveTrue(String code);
}
