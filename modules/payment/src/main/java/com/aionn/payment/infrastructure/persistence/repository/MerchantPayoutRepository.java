package com.aionn.payment.infrastructure.persistence.repository;

import com.aionn.payment.infrastructure.persistence.entity.MerchantPayoutEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MerchantPayoutRepository extends JpaRepository<MerchantPayoutEntity, String> {

    List<MerchantPayoutEntity> findByMerchantIdOrderByRequestedAtDesc(String merchantId, Pageable pageable);

    List<MerchantPayoutEntity> findByStatusOrderByRequestedAtAsc(String status, Pageable pageable);

    java.util.Optional<MerchantPayoutEntity> findFirstByMerchantIdAndStatusOrderByCompletedAtDesc(
            String merchantId, String status);
}
