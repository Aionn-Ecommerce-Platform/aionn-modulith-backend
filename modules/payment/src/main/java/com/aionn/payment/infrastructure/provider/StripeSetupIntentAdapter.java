package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.dto.method.result.StripeSetupIntentResult;
import com.aionn.payment.application.port.out.StripeSetupIntentPort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.infrastructure.provider.config.StripeProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.SetupIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.SetupIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripeSetupIntentAdapter implements StripeSetupIntentPort {

    private final StripeProperties properties;

    @Override
    public StripeSetupIntentResult create(String userId) {
        ensureConfigured();
        try {
            SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                    .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)
                    .addPaymentMethodType("card")
                    .setDescription("Aionn saved card")
                    .putMetadata("userId", userId)
                    .build();
            SetupIntent intent = SetupIntent.create(params, requestOptions());
            return new StripeSetupIntentResult(intent.getId(), intent.getClientSecret());
        } catch (StripeException ex) {
            throw providerFailure("creation", ex);
        }
    }

    @Override
    public CompletedSetupIntent complete(String userId, String setupIntentId) {
        ensureConfigured();
        try {
            SetupIntent intent = SetupIntent.retrieve(setupIntentId, requestOptions());
            String ownerUserId = intent.getMetadata() == null ? null : intent.getMetadata().get("userId");
            if (!userId.equals(ownerUserId)) {
                throw new PaymentException(PaymentErrorCode.METHOD_FORBIDDEN);
            }
            if (!"succeeded".equals(intent.getStatus())) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATE,
                        "Stripe setup intent is not succeeded: " + intent.getStatus());
            }
            String paymentMethodId = intent.getPaymentMethod();
            if (paymentMethodId == null || paymentMethodId.isBlank()) {
                throw new PaymentException(PaymentErrorCode.INVALID_ARGUMENT,
                        "Stripe setup intent has no payment method");
            }
            com.stripe.model.PaymentMethod paymentMethod =
                    com.stripe.model.PaymentMethod.retrieve(paymentMethodId, requestOptions());
            com.stripe.model.PaymentMethod.Card card = paymentMethod.getCard();
            if (card == null) {
                throw new PaymentException(PaymentErrorCode.INVALID_ARGUMENT,
                        "Stripe payment method is not a card");
            }
            return new CompletedSetupIntent(normalizeCardBrand(card.getBrand()), card.getLast4(), paymentMethodId);
        } catch (StripeException ex) {
            throw providerFailure("completion", ex);
        }
    }

    private RequestOptions requestOptions() {
        return RequestOptions.builder().setApiKey(properties.apiKey()).build();
    }

    private void ensureConfigured() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR, "Stripe API key is missing");
        }
    }

    private PaymentException providerFailure(String operation, StripeException ex) {
        log.warn("Stripe setup-intent {} failed: {}", operation, ex.getMessage());
        return new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR, ex.getMessage());
    }

    private static String normalizeCardBrand(String brand) {
        if (brand == null || brand.isBlank()) {
            return "CARD";
        }
        return brand.replace(" ", "_").toUpperCase(Locale.ROOT);
    }
}
