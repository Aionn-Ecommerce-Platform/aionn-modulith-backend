package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.method.command.LinkMethodCommand;
import com.aionn.payment.application.dto.method.command.RemoveMethodCommand;
import com.aionn.payment.application.dto.method.command.VerifyMethodCommand;
import com.aionn.payment.application.dto.method.result.StripeSetupIntentResult;
import com.aionn.payment.application.port.out.PaymentMethodPersistencePort;
import com.aionn.payment.application.port.out.StripeSetupIntentPort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodPersistencePort repository;
    private final EventPublisher eventPublisher;
    private final StripeSetupIntentPort stripeSetupIntentPort;
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
        return stripeSetupIntentPort.create(userId);
    }

    public PaymentMethod completeStripeSetupIntent(String userId, String setupIntentId) {
        StripeSetupIntentPort.CompletedSetupIntent completed = stripeSetupIntentPort.complete(userId, setupIntentId);
        return inTransaction(() -> persistVerifiedMethod(
                userId, completed.provider(), completed.last4(), completed.paymentMethodId()));
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

}
