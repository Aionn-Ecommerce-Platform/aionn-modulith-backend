package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payment.command.RefundPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.mapper.PaymentResultMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentPersistencePort paymentRepository;
    private final PaymentMethodPersistencePort paymentMethodRepository;
    private final TransactionLedgerPersistencePort ledgerRepository;
    private final PaymentProviderRouter providerRouter;
    private final InvoiceStorage invoiceStorage;
    private final PaymentResultMapper mapper;
    private final EventPublisher eventPublisher;
    private final PaymentIntegrationEventPublisherPort integrationEventPublisher;
    private final com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort orderQueryPort;
    private final java.time.Clock clock;

    public PaymentResult initiate(InitiatePaymentCommand command) {
        var existing = paymentRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return mapper.toResult(existing.get());
        }

        PaymentMethod method = null;
        if (command.paymentMethodId() != null) {
            method = paymentMethodRepository.findById(command.paymentMethodId())
                    .orElseThrow(() -> new PaymentException(PaymentErrorCode.METHOD_NOT_FOUND));
            method.ensureOwnedBy(command.userId());
            if (method.getStatus() != PaymentMethodStatus.VERIFIED) {
                throw new PaymentException(PaymentErrorCode.METHOD_NOT_VERIFIED);
            }
        }

        Instant now = clock.instant();
        Money amount = Money.of(command.amount(), command.currency());
        Payment payment = Payment.initiate(IdGenerator.ulid(), command.orderId(), command.userId(),
                command.paymentMethodId(), amount, command.gateway(), command.idempotencyKey(), now);
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publish(payment.pullEvents());

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
            return confirm(new ConfirmPaymentCommand(saved.getPaymentId(), auth.transactionNo()));
        } else if (auth.declineCode() != null) {
            return fail(new FailPaymentCommand(saved.getPaymentId(),
                    auth.declineCode(), auth.declineReason()));
        }
        // Async path: return INITIATED and propagate the redirect URL for client
        // hand-off.
        return mapper.toResult(saved).withRedirectUrl(auth.authUrl());
    }

    public PaymentResult confirm(ConfirmPaymentCommand command) {
        Payment payment = required(command.paymentId());
        if (payment.getStatus().name().equals("PAID")) {
            return mapper.toResult(payment);
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

        Payment current = saved;
        try {
            String invoiceUrl = invoiceStorage.storeInvoiceUrl(saved.getPaymentId(), saved.getOrderId());
            saved.attachInvoice(invoiceUrl, now);
            current = paymentRepository.save(saved);
            eventPublisher.publish(saved.pullEvents());
        } catch (RuntimeException ex) {
            log.warn("Invoice attachment failed for payment {}: {}. Order capture will still be published.",
                    saved.getPaymentId(), ex.getMessage());
        }

        integrationEventPublisher.publishPaymentCaptured(current.getPaymentId(), current.getOrderId(),
                command.transactionNo(), current.getAmount().amount(), current.getAmount().currency());
        return mapper.toResult(current);
    }

    public PaymentResult fail(FailPaymentCommand command) {
        Payment payment = required(command.paymentId());
        if (payment.getStatus().name().equals("FAILED") || payment.getStatus().name().equals("PAID")
                || payment.getStatus().name().equals("REFUNDED")) {
            return mapper.toResult(payment);
        }
        Instant now = clock.instant();
        payment.markFailed(command.errorCode(), command.reason(), now);
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publish(payment.pullEvents());
        integrationEventPublisher.publishPaymentFailed(saved.getPaymentId(), saved.getOrderId(),
                command.errorCode(), command.reason());
        return mapper.toResult(saved);
    }

    public PaymentResult refund(RefundPaymentCommand command) {
        Payment payment = required(command.paymentId());
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
        return mapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResult get(String paymentId) {
        return mapper.toResult(required(paymentId));
    }

    @Transactional(readOnly = true)
    public PaymentResult getForUser(String paymentId, String userId) {
        Payment payment = required(paymentId);
        if (!payment.getUserId().equals(userId)) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        return mapper.toResult(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResult> listByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(mapper::toResult)
                .toList();
    }

    Payment required(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }
}
