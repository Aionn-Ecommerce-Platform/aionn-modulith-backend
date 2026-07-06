package com.aionn.catalog.infrastructure.persistence.repository.merchant;

import com.aionn.catalog.infrastructure.persistence.entity.MerchantEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<MerchantEntity, String> {

    Optional<MerchantEntity> findByOwnerId(String ownerId);

    boolean existsByOwnerId(String ownerId);

    List<MerchantEntity> findAllBy(Pageable pageable);
}

