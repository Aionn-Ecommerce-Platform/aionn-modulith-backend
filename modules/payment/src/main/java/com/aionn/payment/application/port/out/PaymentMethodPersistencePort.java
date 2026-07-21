package com.aionn.payment.application.port.out;

import com.aionn.payment.domain.model.PaymentMethod;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodPersistencePort {

    PaymentMethod save(PaymentMethod method);

    Optional<PaymentMethod> findById(String methodId);

    List<PaymentMethod> findActiveByUserId(String userId);
}

