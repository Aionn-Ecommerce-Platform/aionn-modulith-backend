package com.aionn.payment.application.port.out;

import com.aionn.payment.domain.model.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentPersistencePort {

    Payment save(Payment payment);

    Optional<Payment> findById(String paymentId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByOrderId(String orderId);
}

