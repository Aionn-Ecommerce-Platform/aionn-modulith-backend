package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.payment.PaymentInitiation;
import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payment.command.RefundPaymentCommand;
import com.aionn.payment.application.port.out.InvoiceStorage;
import com.aionn.payment.application.port.out.PaymentMethodPersistencePort;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import com.aionn.payment.application.port.out.PaymentPersistencePort;
import com.aionn.payment.application.port.out.TransactionLedgerPersistencePort;
import com.aionn.payment.application.port.out.integration.PaymentIntegrationEventPublisherPort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.model.Payment;
import com.aionn.payment.domain.model.PaymentMethod;
import com.aionn.payment.domain.model.TransactionLedger;
import com.aionn.payment.domain.valueobject.LedgerEntryType;
import com.aionn.payment.domain.valueobject.PaymentMethodStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
public class PaymentService {

    private final PaymentPersistencePort paymentRepository;
    private final PaymentMethodPersistencePort paymentMethodRepository;
    private final TransactionLedgerPersistencePort ledgerRepository;
    private final PaymentProviderRouter providerRouter;
    private final InvoiceStorage invoiceStorage;
    private final EventPublisher eventPublisher;
    private final PaymentIntegrationEventPublisherPort integrationEventPublisher;
    private final com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort orderQueryPort;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public PaymentService(PaymentPersistencePort paymentRepository,
            PaymentMethodPersistencePort paymentMethodRepository,
            TransactionLedgerPersistencePort ledgerRepository, PaymentProviderRouter providerRouter,
            InvoiceStorage invoiceStorage, EventPublisher eventPublisher,
            PaymentIntegrationEventPublisherPort integrationEventPublisher,
            com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort orderQueryPort, Clock clock,
            TransactionTemplate transactionTemplate) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.ledgerRepository = ledgerRepository;
        this.providerRouter = providerRouter;
        this.invoiceStorage = invoiceStorage;
        this.eventPublisher = eventPublisher;
        this.integrationEventPublisher = integrationEventPublisher;
        this.orderQueryPort = orderQueryPort;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentInitiation initiate(InitiatePaymentCommand command) {
        var existing = inTransaction(() -> paymentRepository.findByIdempotencyKey(command.idempotencyKey()));
        if (existing.isPresent()) {
            return new PaymentInitiation(existing.get(), null);
        }

        PaymentMethod method = command.paymentMethodId() == null ? null : inTransaction(() -> {
            PaymentMethod selected = paymentMethodRepository.findById(command.paymentMethodId())
                    .orElseThrow(() -> new PaymentException(PaymentErrorCode.METHOD_NOT_FOUND));
            selected.ensureOwnedBy(command.userId());
            if (selected.getStatus() != PaymentMethodStatus.VERIFIED) {
                throw new PaymentException(PaymentErrorCode.METHOD_NOT_VERIFIED);
            }
            return selected;
        });

        Instant now = clock.instant();
        Money amount = Money.of(command.amount(), command.currency());
        Payment payment = Payment.initiate(IdGenerator.ulid(), command.orderId(), command.userId(),
                command.paymentMethodId(), amount, command.gateway(), command.idempotencyKey(), now);
        Payment saved = inTransaction(() -> {
            Payment persisted = paymentRepository.save(payment);
            eventPublisher.publish(payment.pullEvents());
            return persisted;
        });

        PaymentProviderClient client = providerRouter.route(command.gateway());
        String merchantId = orderQueryPort.findOrderSummary(command.orderId())
                .map(s -> s.merchantId()).orElse(null);
        PaymentProviderClient.Authorization auth = client.authorize(
                new PaymentProviderClient.AuthorizationRequest(
                        saved.getPaymentId(), command.orderId(), command.userId(),
                        merchantId,
                        method == null ? null : method.getGatewayToken(),
                        command.amount(), command.currency(), command.idempotencyKey(), null));

        if (auth.captured()) {
            Payment confirmed = confirm(new ConfirmPaymentCommand(saved.getPaymentId(), auth.transactionNo()));
            return new PaymentInitiation(confirmed, null);
        } else if (auth.declineCode() != null) {
            Payment failed = fail(new FailPaymentCommand(saved.getPaymentId(),
                    auth.declineCode(), auth.declineReason()));
            return new PaymentInitiation(failed, null);
        }
        return new PaymentInitiation(saved, auth.authUrl());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Payment confirm(ConfirmPaymentCommand command) {
        Payment existing = inTransaction(() -> required(command.paymentId()));
        if (existing.getStatus().name().equals("PAID")) {
            return existing;
        }
        Payment payment = inTransaction(() -> confirmInTransaction(command));
        Payment current = payment;
        try {
            String invoiceUrl = invoiceStorage.storeInvoiceUrl(payment.getPaymentId(), payment.getOrderId());
            current = inTransaction(() -> attachInvoice(command.paymentId(), invoiceUrl));
        } catch (RuntimeException ex) {
            log.warn("Invoice attachment failed for payment {}: {}. Order capture will still be published.",
                    payment.getPaymentId(), ex.getMessage());
        }

        Payment captured = current;
        inTransaction(() -> {
            integrationEventPublisher.publishPaymentCaptured(captured.getPaymentId(), captured.getOrderId(),
                    command.transactionNo(), captured.getAmount().amount(), captured.getAmount().currency());
            return null;
        });
        return current;
    }

    private Payment confirmInTransaction(ConfirmPaymentCommand command) {
        Payment payment = required(command.paymentId());
        if (payment.getStatus().name().equals("PAID")) {
            return payment;
        }
        Instant now = clock.instant();
        payment.markPaid(command.transactionNo(), now);
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publish(payment.pullEvents());

        TransactionLedger entry = TransactionLedger.record(IdGenerator.ulid(),
                saved.getPaymentId(), saved.getAmount(), LedgerEntryType.CREDIT,
                saved.getGateway().name(), command.transactionNo(), now);
        ledgerRepository.save(entry);
        eventPublisher.publish(entry.pullEvents());

        return saved;
    }

    private Payment attachInvoice(String paymentId, String invoiceUrl) {
        Payment payment = required(paymentId);
        payment.attachInvoice(invoiceUrl, clock.instant());
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publish(payment.pullEvents());
        return saved;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Payment fail(FailPaymentCommand command) {
        return inTransaction(() -> failInTransaction(command));
    }

    private Payment failInTransaction(FailPaymentCommand command) {
        Payment payment = required(command.paymentId());
        if (payment.getStatus().name().equals("FAILED") || payment.getStatus().name().equals("PAID")
                || payment.getStatus().name().equals("REFUNDED")) {
            return payment;
        }
        Instant now = clock.instant();
        payment.markFailed(command.errorCode(), command.reason(), now);
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publish(payment.pullEvents());
        integrationEventPublisher.publishPaymentFailed(saved.getPaymentId(), saved.getOrderId(),
                command.errorCode(), command.reason());
        return saved;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Payment refund(RefundPaymentCommand command) {
        Payment payment = inTransaction(() -> required(command.paymentId()));
        Money refund = Money.of(command.amount(), command.currency());

        PaymentProviderClient client = providerRouter.route(payment.getGateway());
        PaymentProviderClient.Refund providerRefund = client.refund(new PaymentProviderClient.RefundRequest(
                payment.getPaymentId(), payment.getTransactionNo(), command.amount(), command.currency(),
                command.reason()));
        if (!providerRefund.accepted()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR,
                    "Refund declined: " + providerRefund.declineReason());
        }

        String refundId = providerRefund.refundTransactionNo() != null
                ? providerRefund.refundTransactionNo()
                : "refund-" + IdGenerator.ulid();
        Payment saved = inTransaction(() -> persistRefund(command, refund, refundId));

        return saved;
    }

    public Payment get(String paymentId) {
        return inTransaction(() -> required(paymentId));
    }

    public Payment getForUser(String paymentId, String userId) {
        Payment payment = inTransaction(() -> required(paymentId));
        if (!payment.getUserId().equals(userId)) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        return payment;
    }

    public List<Payment> listByOrderId(String orderId) {
        return inTransaction(() -> paymentRepository.findByOrderId(orderId));
    }

    private Payment persistRefund(RefundPaymentCommand command, Money refund, String refundId) {
        Payment payment = required(command.paymentId());
        Instant now = clock.instant();
        payment.refund(refundId, refund, command.reason(), now);
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publish(payment.pullEvents());

        TransactionLedger entry = TransactionLedger.record(IdGenerator.ulid(),
                saved.getPaymentId(), refund, LedgerEntryType.DEBIT,
                saved.getGateway().name(), refundId, now);
        ledgerRepository.save(entry);
        eventPublisher.publish(entry.pullEvents());
        integrationEventPublisher.publishPaymentRefunded(saved.getPaymentId(), saved.getOrderId(),
                refundId, command.amount(), command.currency(), command.reason());
        return saved;
    }

    private <T> T inTransaction(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }

    Payment required(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }
}
