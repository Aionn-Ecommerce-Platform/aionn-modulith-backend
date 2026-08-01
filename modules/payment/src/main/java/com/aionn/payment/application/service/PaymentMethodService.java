package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.method.command.LinkMethodCommand;
import com.aionn.payment.application.dto.method.command.RemoveMethodCommand;
import com.aionn.payment.application.dto.method.command.VerifyMethodCommand;
import com.aionn.payment.application.dto.method.result.StripeSetupIntentResult;
import com.aionn.payment.application.port.out.PaymentMethodPersistencePort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.infrastructure.provider.config.StripeProperties;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import com.stripe.exception.StripeException;
import com.stripe.model.SetupIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.SetupIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodPersistencePort repository;
    private final EventPublisher eventPublisher;
    private final StripeProperties stripeProperties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public PaymentMethod link(LinkMethodCommand command) {
        return inTransaction(() -> linkInTransaction(command));
    }

    private PaymentMethod linkInTransaction(LinkMethodCommand command) {
        Instant now = clock.instant();
        PaymentMethod method = PaymentMethod.link(IdGenerator.ulid(),
                command.userId(), command.provider(), command.last4Digits(), command.gatewayToken(), now);
        PaymentMethod saved = repository.save(method);
        eventPublisher.publish(method.pullEvents());
        return saved;
    }

    public PaymentMethod verify(VerifyMethodCommand command) {
        return inTransaction(() -> verifyInTransaction(command));
    }

    private PaymentMethod verifyInTransaction(VerifyMethodCommand command) {
        PaymentMethod method = ownedBy(command.methodId(), command.userId());
        Instant now = clock.instant();
        method.verify(now);
        PaymentMethod saved = repository.save(method);
        eventPublisher.publish(method.pullEvents());
        return saved;
    }

    public StripeSetupIntentResult createStripeSetupIntent(String userId) {
        ensureStripeConfigured();
        try {
            SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                    .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)
                    .addPaymentMethodType("card")
                    .setDescription("Aionn saved card")
                    .putMetadata("userId", userId)
                    .build();
            SetupIntent intent = SetupIntent.create(params, stripeRequestOptions());
            return new StripeSetupIntentResult(intent.getId(), intent.getClientSecret());
        } catch (StripeException ex) {
            log.warn("Stripe setup-intent creation failed: {}", ex.getMessage());
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR, ex.getMessage());
        }
    }

    public PaymentMethod completeStripeSetupIntent(String userId, String setupIntentId) {
        ensureStripeConfigured();
        try {
            SetupIntent intent = SetupIntent.retrieve(setupIntentId, stripeRequestOptions());
            String ownerUserId = intent.getMetadata() == null ? null : intent.getMetadata().get("userId");
            if (!userId.equals(ownerUserId)) {
                throw new PaymentException(PaymentErrorCode.METHOD_FORBIDDEN);
            }
            if (!"succeeded".equals(intent.getStatus())) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATE,
                        "Stripe setup intent is not succeeded: " + intent.getStatus());
            }
            String stripePaymentMethodId = intent.getPaymentMethod();
            if (stripePaymentMethodId == null || stripePaymentMethodId.isBlank()) {
                throw new PaymentException(PaymentErrorCode.INVALID_ARGUMENT,
                        "Stripe setup intent has no payment method");
            }

            com.stripe.model.PaymentMethod stripeMethod =
                    com.stripe.model.PaymentMethod.retrieve(stripePaymentMethodId, stripeRequestOptions());
            com.stripe.model.PaymentMethod.Card card = stripeMethod.getCard();
            if (card == null) {
                throw new PaymentException(PaymentErrorCode.INVALID_ARGUMENT,
                        "Stripe payment method is not a card");
            }

            String provider = normalizeCardBrand(card.getBrand());
            return inTransaction(() -> persistVerifiedMethod(
                    userId, provider, card.getLast4(), stripePaymentMethodId));
        } catch (StripeException ex) {
            log.warn("Stripe setup-intent completion failed: {}", ex.getMessage());
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR, ex.getMessage());
        }
    }

    public void remove(RemoveMethodCommand command) {
        inTransaction(() -> {
            removeInTransaction(command);
            return null;
        });
    }

    private void removeInTransaction(RemoveMethodCommand command) {
        PaymentMethod method = ownedBy(command.methodId(), command.userId());
        Instant now = clock.instant();
        method.remove(now);
        repository.save(method);
        eventPublisher.publish(method.pullEvents());
    }

    public List<PaymentMethod> listMine(String userId) {
        return inTransaction(() -> repository.findActiveByUserId(userId));
    }

    public PaymentMethod get(String userId, String methodId) {
        return inTransaction(() -> ownedBy(methodId, userId));
    }

    private PaymentMethod persistVerifiedMethod(String userId, String provider, String last4,
            String stripePaymentMethodId) {
        Instant now = clock.instant();
        PaymentMethod method = PaymentMethod.link(IdGenerator.ulid(),
                userId, provider, last4, stripePaymentMethodId, now);
        method.verify(now);
        PaymentMethod saved = repository.save(method);
        eventPublisher.publish(method.pullEvents());
        return saved;
    }

    private <T> T inTransaction(java.util.function.Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }

    private PaymentMethod ownedBy(String methodId, String userId) {
        PaymentMethod method = repository.findById(methodId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.METHOD_NOT_FOUND));
        method.ensureOwnedBy(userId);
        return method;
    }

    private RequestOptions stripeRequestOptions() {
        return RequestOptions.builder()
                .setApiKey(stripeProperties.apiKey())
                .build();
    }

    private void ensureStripeConfigured() {
        if (stripeProperties.apiKey() == null || stripeProperties.apiKey().isBlank()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR,
                    "Stripe API key is missing");
        }
    }

    private static String normalizeCardBrand(String brand) {
        if (brand == null || brand.isBlank()) {
            return "CARD";
        }
        return brand.replace(" ", "_").toUpperCase(Locale.ROOT);
    }
}
