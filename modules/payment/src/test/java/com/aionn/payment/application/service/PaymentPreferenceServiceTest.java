package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.preference.result.PaymentPreferenceResult;
import com.aionn.payment.application.port.out.PaymentMethodPersistencePort;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.domain.valueobject.PaymentMethodStatus;
import com.aionn.payment.infrastructure.persistence.entity.PaymentPreferenceEntity;
import com.aionn.payment.infrastructure.persistence.repository.PaymentPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentPreferenceServiceTest {

    @Mock
    private PaymentPreferenceRepository preferenceRepository;
    @Mock
    private PaymentMethodPersistencePort paymentMethodRepository;

    private PaymentPreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService = new PaymentPreferenceService(preferenceRepository, paymentMethodRepository);
    }

    @Test
    void shouldReturnCodWhenNoPreferenceFound() {
        when(preferenceRepository.findById("user-1")).thenReturn(Optional.empty());

        PaymentPreferenceResult result = preferenceService.get("user-1");

        assertEquals("COD", result.paymentType());
        assertNull(result.paymentMethodId());
    }

    @Test
    void shouldReturnCodWhenPreferenceIsCod() {
        PaymentPreferenceEntity entity = new PaymentPreferenceEntity();
        entity.setUserId("user-1");
        entity.setPaymentType("COD");
        when(preferenceRepository.findById("user-1")).thenReturn(Optional.of(entity));

        PaymentPreferenceResult result = preferenceService.get("user-1");

        assertEquals("COD", result.paymentType());
        assertNull(result.paymentMethodId());
    }

    @Test
    void shouldReturnSavedCardWhenCardIsUsable() {
        PaymentPreferenceEntity entity = new PaymentPreferenceEntity();
        entity.setUserId("user-1");
        entity.setPaymentType("SAVED_CARD");
        entity.setPaymentMethodId("card-1");
        when(preferenceRepository.findById("user-1")).thenReturn(Optional.of(entity));

        PaymentMethod card = new PaymentMethod("card-1", "user-1", "VISA", "4242", "tok-1", PaymentMethodStatus.VERIFIED, Instant.now(), Instant.now(), Instant.now());
        when(paymentMethodRepository.findById("card-1")).thenReturn(Optional.of(card));

        PaymentPreferenceResult result = preferenceService.get("user-1");

        assertEquals("SAVED_CARD", result.paymentType());
        assertEquals("card-1", result.paymentMethodId());
    }

    @Test
    void shouldResetToCodWhenCardIsNotUsable() {
        PaymentPreferenceEntity entity = new PaymentPreferenceEntity();
        entity.setUserId("user-1");
        entity.setPaymentType("SAVED_CARD");
        entity.setPaymentMethodId("card-1");
        when(preferenceRepository.findById("user-1")).thenReturn(Optional.of(entity));

        when(paymentMethodRepository.findById("card-1")).thenReturn(Optional.empty());

        PaymentPreferenceResult result = preferenceService.get("user-1");

        assertEquals("COD", result.paymentType());
        assertNull(result.paymentMethodId());
        verify(preferenceRepository).save(entity);
        assertEquals("COD", entity.getPaymentType());
        assertNull(entity.getPaymentMethodId());
    }

    @Test
    void shouldUpdateToCod() {
        PaymentPreferenceEntity entity = new PaymentPreferenceEntity();
        entity.setUserId("user-1");
        when(preferenceRepository.findById("user-1")).thenReturn(Optional.of(entity));

        PaymentPreferenceResult result = preferenceService.update("user-1", "COD", null);

        assertEquals("COD", result.paymentType());
        verify(preferenceRepository).save(entity);
    }

    @Test
    void shouldUpdateToVnpay() {
        PaymentPreferenceEntity entity = new PaymentPreferenceEntity();
        entity.setUserId("user-1");
        when(preferenceRepository.findById("user-1")).thenReturn(Optional.of(entity));

        PaymentPreferenceResult result = preferenceService.update("user-1", "VNPAY", null);

        assertEquals("VNPAY", result.paymentType());
        verify(preferenceRepository).save(entity);
    }

    @Test
    void shouldUpdateToSavedCardSuccessfully() {
        PaymentPreferenceEntity entity = new PaymentPreferenceEntity();
        entity.setUserId("user-1");
        when(preferenceRepository.findById("user-1")).thenReturn(Optional.of(entity));

        PaymentMethod card = new PaymentMethod("card-1", "user-1", "VISA", "4242", "tok-1", PaymentMethodStatus.VERIFIED, Instant.now(), Instant.now(), Instant.now());
        when(paymentMethodRepository.findById("card-1")).thenReturn(Optional.of(card));

        PaymentPreferenceResult result = preferenceService.update("user-1", "SAVED_CARD", "card-1");

        assertEquals("SAVED_CARD", result.paymentType());
        assertEquals("card-1", result.paymentMethodId());
        verify(preferenceRepository).save(entity);
    }

    @Test
    void shouldFailToUpdateToSavedCardWhenNotVerified() {
        PaymentPreferenceEntity entity = new PaymentPreferenceEntity();
        entity.setUserId("user-1");
        when(preferenceRepository.findById("user-1")).thenReturn(Optional.of(entity));

        PaymentMethod card = new PaymentMethod("card-1", "user-1", "VISA", "4242", "tok-1", PaymentMethodStatus.LINKED, Instant.now(), Instant.now(), null);
        when(paymentMethodRepository.findById("card-1")).thenReturn(Optional.of(card));

        assertThrows(PaymentException.class, () -> preferenceService.update("user-1", "SAVED_CARD", "card-1"));
    }
}
