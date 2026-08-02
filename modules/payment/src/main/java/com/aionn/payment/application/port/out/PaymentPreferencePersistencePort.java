package com.aionn.payment.application.port.out;

import java.util.Optional;

public interface PaymentPreferencePersistencePort {

    Optional<Preference> findByUserId(String userId);

    Preference save(Preference preference);

    record Preference(String userId, String paymentType, String paymentMethodId) {}
}
