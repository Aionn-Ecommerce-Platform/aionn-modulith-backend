package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.preference.result.PaymentPreferenceResult;
import com.aionn.payment.application.port.out.PaymentMethodPersistencePort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentMethodStatus;
import com.aionn.payment.infrastructure.persistence.entity.PaymentPreferenceEntity;
import com.aionn.payment.infrastructure.persistence.repository.PaymentPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentPreferenceService {

    private final PaymentPreferenceRepository preferenceRepository;
    private final PaymentMethodPersistencePort paymentMethodRepository;

    public PaymentPreferenceResult get(String userId) {
        PaymentPreferenceEntity preference = preferenceRepository.findById(userId).orElse(null);
        if (preference == null || "COD".equals(preference.getPaymentType())) {
            return cod();
        }

        if (!isUsableMethod(userId, preference.getPaymentMethodId())) {
            preference.setPaymentType("COD");
            preference.setPaymentMethodId(null);
            preferenceRepository.save(preference);
            return cod();
        }
        return new PaymentPreferenceResult("SAVED_CARD", preference.getPaymentMethodId());
    }

    public PaymentPreferenceResult update(String userId, String paymentType, String paymentMethodId) {
        PaymentPreferenceEntity preference = preferenceRepository.findById(userId).orElseGet(() -> {
            PaymentPreferenceEntity created = new PaymentPreferenceEntity();
            created.setUserId(userId);
            return created;
        });

        if ("COD".equalsIgnoreCase(paymentType)) {
            preference.setPaymentType("COD");
            preference.setPaymentMethodId(null);
            preferenceRepository.save(preference);
            return cod();
        }
        if ("VNPAY".equalsIgnoreCase(paymentType)) {
            preference.setPaymentType("VNPAY");
            preference.setPaymentMethodId(null);
            preferenceRepository.save(preference);
            return new PaymentPreferenceResult("VNPAY", null);
        }
        if (!"SAVED_CARD".equalsIgnoreCase(paymentType) || !isUsableMethod(userId, paymentMethodId)) {
            throw new PaymentException(PaymentErrorCode.INVALID_ARGUMENT,
                    "A verified saved card is required for this payment preference");
        }

        preference.setPaymentType("SAVED_CARD");
        preference.setPaymentMethodId(paymentMethodId);
        preferenceRepository.save(preference);
        return new PaymentPreferenceResult("SAVED_CARD", paymentMethodId);
    }

    private boolean isUsableMethod(String userId, String methodId) {
        if (methodId == null || methodId.isBlank()) {
            return false;
        }
        return paymentMethodRepository.findById(methodId)
                .filter(method -> userId.equals(method.getUserId()))
                .map(method -> method.getStatus() == PaymentMethodStatus.VERIFIED)
                .orElse(false);
    }

    private PaymentPreferenceResult cod() {
        return new PaymentPreferenceResult("COD", null);
    }
}
