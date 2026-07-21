package com.aionn.payment.infrastructure.persistence.repository;

import com.aionn.payment.infrastructure.persistence.entity.PaymentPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentPreferenceRepository extends JpaRepository<PaymentPreferenceEntity, String> {
}
