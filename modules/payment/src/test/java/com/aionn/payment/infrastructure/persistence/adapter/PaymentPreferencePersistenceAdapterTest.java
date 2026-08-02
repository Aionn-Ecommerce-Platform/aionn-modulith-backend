package com.aionn.payment.infrastructure.persistence.adapter;

import com.aionn.payment.application.port.out.PaymentPreferencePersistencePort.Preference;
import com.aionn.payment.infrastructure.persistence.entity.PaymentPreferenceEntity;
import com.aionn.payment.infrastructure.persistence.repository.PaymentPreferenceRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentPreferencePersistenceAdapterTest {

    private final PaymentPreferenceRepository repository = mock(PaymentPreferenceRepository.class);
    private final PaymentPreferencePersistenceAdapter adapter = new PaymentPreferencePersistenceAdapter(repository);

    @Test
    void findsAndMapsPreference() {
        PaymentPreferenceEntity entity = entity("user-1", "SAVED_CARD", "pm-1");
        when(repository.findById("user-1")).thenReturn(Optional.of(entity));

        assertThat(adapter.findByUserId("user-1")).contains(new Preference("user-1", "SAVED_CARD", "pm-1"));
    }

    @Test
    void savesNewPreference() {
        when(repository.findById("user-1")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(adapter.save(new Preference("user-1", "COD", null)))
                .isEqualTo(new Preference("user-1", "COD", null));
    }

    private static PaymentPreferenceEntity entity(String userId, String type, String methodId) {
        PaymentPreferenceEntity entity = new PaymentPreferenceEntity();
        entity.setUserId(userId);
        entity.setPaymentType(type);
        entity.setPaymentMethodId(methodId);
        return entity;
    }
}
