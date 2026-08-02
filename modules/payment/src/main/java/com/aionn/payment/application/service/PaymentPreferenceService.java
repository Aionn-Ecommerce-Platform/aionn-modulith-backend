package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.preference.result.PaymentPreferenceResult;
import com.aionn.payment.application.port.out.PaymentMethodPersistencePort;
import com.aionn.payment.application.port.out.PaymentPreferencePersistencePort;
import com.aionn.payment.application.port.out.PaymentPreferencePersistencePort.Preference;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentMethodStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentPreferenceService {

    private static final String SAVED_CARD_TYPE = "SAVED_CARD";
    private static final String VNPAY_TYPE = "VNPAY";

    private final PaymentPreferencePersistencePort preferenceRepository;
    private final PaymentMethodPersistencePort paymentMethodRepository;

    public PaymentPreferenceResult get(String userId) {
        Preference preference = preferenceRepository.findByUserId(userId).orElse(null);
        if (preference == null || "COD".equals(preference.paymentType())) {
            return cod();
        }

        if (!isUsableMethod(userId, preference.paymentMethodId())) {
            preferenceRepository.save(new Preference(userId, "COD", null));
            return cod();
        }
        return new PaymentPreferenceResult(SAVED_CARD_TYPE, preference.paymentMethodId());
    }

    public PaymentPreferenceResult update(String userId, String paymentType, String paymentMethodId) {
        if ("COD".equalsIgnoreCase(paymentType)) {
            preferenceRepository.save(new Preference(userId, "COD", null));
            return cod();
        }
        if (VNPAY_TYPE.equalsIgnoreCase(paymentType)) {
            preferenceRepository.save(new Preference(userId, VNPAY_TYPE, null));
            return new PaymentPreferenceResult(VNPAY_TYPE, null);
        }
        if (!SAVED_CARD_TYPE.equalsIgnoreCase(paymentType) || !isUsableMethod(userId, paymentMethodId)) {
            throw new PaymentException(PaymentErrorCode.INVALID_ARGUMENT,
                    "A verified saved card is required for this payment preference");
        }

        preferenceRepository.save(new Preference(userId, SAVED_CARD_TYPE, paymentMethodId));
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
