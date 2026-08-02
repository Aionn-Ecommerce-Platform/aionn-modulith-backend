package com.aionn.payment.infrastructure.persistence.adapter;

import com.aionn.payment.application.port.out.PaymentPreferencePersistencePort;
import com.aionn.payment.infrastructure.persistence.entity.PaymentPreferenceEntity;
import com.aionn.payment.infrastructure.persistence.repository.PaymentPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentPreferencePersistenceAdapter implements PaymentPreferencePersistencePort {

    private final PaymentPreferenceRepository repository;

    @Override
    public Optional<Preference> findByUserId(String userId) {
        return repository.findById(userId).map(this::toPreference);
    }

    @Override
    public Preference save(Preference preference) {
        PaymentPreferenceEntity entity = repository.findById(preference.userId())
                .orElseGet(PaymentPreferenceEntity::new);
        entity.setUserId(preference.userId());
        entity.setPaymentType(preference.paymentType());
        entity.setPaymentMethodId(preference.paymentMethodId());
        return toPreference(repository.save(entity));
    }

    private Preference toPreference(PaymentPreferenceEntity entity) {
        return new Preference(entity.getUserId(), entity.getPaymentType(), entity.getPaymentMethodId());
    }
}
