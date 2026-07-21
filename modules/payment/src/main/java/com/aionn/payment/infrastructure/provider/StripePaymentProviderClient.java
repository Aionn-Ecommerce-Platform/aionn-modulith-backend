package com.aionn.payment.infrastructure.provider;

import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.infrastructure.provider.config.StripeProperties;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripePaymentProviderClient implements PaymentProviderClient {

    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA", "PYG",
            "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF");

    private final StripeProperties properties;
    private final MerchantQueryPort merchantQueryPort;

    @PostConstruct
    void init() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "Stripe API key is missing. Set STRIPE_API_KEY in the environment.");
        }
        Stripe.apiKey = properties.apiKey();
    }

    @Override
    public PaymentGatewayKind kind() {
        return PaymentGatewayKind.STRIPE;
    }

    @Override
    public Authorization authorize(AuthorizationRequest request) {
        long minorAmount = toMinorUnits(request.amount(), request.currency());
        try {
            PaymentIntentCreateParams.Builder params = PaymentIntentCreateParams.builder()
                    .setAmount(minorAmount)
                    .setCurrency(request.currency().toLowerCase())
                    .setConfirm(true)
                    .setDescription("Aionn order " + request.orderId())
                    .putMetadata("orderId", request.orderId())
                    .putMetadata("userId", request.userId())
                    .putMetadata("paymentId", request.paymentId());

            if (request.merchantId() != null) {
                merchantQueryPort.findStripeConnectInfo(request.merchantId())
                        .filter(info -> info.chargesEnabled())
                        .ifPresent(info -> {
                            BigDecimal rate = merchantQueryPort.findCommissionRate(request.merchantId())
                                    .orElse(new BigDecimal("0.0500"));
                            long fee = toMinorUnits(
                                    request.amount().multiply(rate).setScale(2, RoundingMode.HALF_UP),
                                    request.currency());
                            params.setApplicationFeeAmount(fee);
                            params.setTransferData(
                                    PaymentIntentCreateParams.TransferData.builder()
                                            .setDestination(info.stripeAccountId())
                                            .build());
                        });
            }
            if (request.paymentMethodToken() != null && !request.paymentMethodToken().isBlank()) {
                params.setPaymentMethod(request.paymentMethodToken());
            }
            if (request.returnUrl() != null && !request.returnUrl().isBlank()) {
                params.setReturnUrl(request.returnUrl());
            }
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(request.idempotencyKey())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params.build(), options);

            String status = intent.getStatus();
            log.info("Stripe authorize: orderId={} intent={} status={}",
                    request.orderId(), intent.getId(), status);

            return switch (status) {
                case "succeeded" -> new Authorization(true, intent.getId(), null, null, null);
                case "requires_action" -> new Authorization(false, intent.getId(),
                        intent.getNextAction() != null && intent.getNextAction().getRedirectToUrl() != null
                                ? intent.getNextAction().getRedirectToUrl().getUrl()
                                : null,
                        null, null);
                case "requires_payment_method" -> new Authorization(false, intent.getId(), null,
                        "STRIPE_CARD_DECLINED", "Card was declined");
                default -> new Authorization(false, intent.getId(), null,
                        "STRIPE_UNEXPECTED_STATUS", "Unexpected PaymentIntent status: " + status);
            };
        } catch (StripeException ex) {
            log.warn("Stripe authorize failed: {}", ex.getMessage());
            return new Authorization(false, null, null,
                    ex.getCode() == null ? "STRIPE_ERROR" : "STRIPE_" + ex.getCode().toUpperCase(),
                    ex.getMessage());
        }
    }

    @Override
    public PaymentProviderClient.Refund refund(RefundRequest request) {
        if (request.transactionNo() == null || request.transactionNo().isBlank()) {
            return new PaymentProviderClient.Refund(false, null,
                    "Cannot refund: original PaymentIntent id is missing");
        }
        try {
            long minorAmount = toMinorUnits(request.amount(), request.currency());
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(request.transactionNo())
                    .setAmount(minorAmount)
                    .putMetadata("paymentId", request.paymentId())
                    .putMetadata("reason", request.reason() == null ? "" : request.reason())
                    .build();
            com.stripe.model.Refund stripeRefund = com.stripe.model.Refund.create(params);
            log.info("Stripe refund: paymentId={} stripeRefund={} status={}",
                    request.paymentId(), stripeRefund.getId(), stripeRefund.getStatus());
            boolean accepted = "succeeded".equals(stripeRefund.getStatus())
                    || "pending".equals(stripeRefund.getStatus());
            return new PaymentProviderClient.Refund(accepted, stripeRefund.getId(),
                    accepted ? null : "Refund status: " + stripeRefund.getStatus());
        } catch (StripeException ex) {
            log.warn("Stripe refund failed: {}", ex.getMessage());
            return new PaymentProviderClient.Refund(false, null, ex.getMessage());
        }
    }

    @Override
    public String generateInvoice(String paymentId, String orderId, BigDecimal amount, String currency) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentId);
            if (intent.getInvoice() != null) {
                Invoice invoice = Invoice.retrieve(intent.getInvoice());
                if (invoice.getHostedInvoiceUrl() != null) {
                    return invoice.getHostedInvoiceUrl();
                }
            }
            if (intent.getLatestCharge() != null) {
                return "https://dashboard.stripe.com/payments/" + intent.getId();
            }
        } catch (StripeException ex) {
            log.debug("Stripe invoice lookup skipped: {}", ex.getMessage());
        }
        return "https://dashboard.stripe.com/payments/" + paymentId;
    }

    @Override
    public WebhookEvent verifyAndParse(String rawBody, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return errorEvent("MISSING_SIGNATURE", "Stripe-Signature header is required");
        }
        Event event;
        try {
            event = Webhook.constructEvent(rawBody, signatureHeader, properties.webhookSecret());
        } catch (SignatureVerificationException ex) {
            log.warn("Stripe webhook signature mismatch: {}", ex.getMessage());
            return errorEvent("SIGNATURE_INVALID", "Webhook signature mismatch");
        } catch (Exception ex) {
            log.warn("Stripe webhook parse failed: {}", ex.getMessage());
            return errorEvent("PARSE_FAILED", ex.getMessage());
        }

        return switch (event.getType()) {
            case "payment_intent.succeeded" -> intentEvent(event, true, null, null);
            case "payment_intent.payment_failed" -> intentEvent(event, false,
                    "STRIPE_PAYMENT_FAILED", "PaymentIntent failed");
            case "payment_intent.canceled" -> intentEvent(event, false,
                    "STRIPE_PAYMENT_CANCELED", "PaymentIntent canceled");
            default -> new WebhookEvent("stripe." + event.getType(), null, null,
                    null, null, true, null, null);
        };
    }

    private WebhookEvent intentEvent(Event event, boolean success, String errorCode, String errorReason) {
        PaymentIntent intent = event.getDataObjectDeserializer().getObject()
                .filter(PaymentIntent.class::isInstance)
                .map(PaymentIntent.class::cast)
                .orElse(null);
        if (intent == null) {
            return errorEvent("PAYLOAD_MALFORMED", "PaymentIntent payload could not be parsed");
        }
        String paymentId = intent.getMetadata() == null ? null : intent.getMetadata().get("paymentId");
        BigDecimal amount = intent.getAmount() == null
                ? null
                : fromMinorUnits(intent.getAmount(), intent.getCurrency());
        return new WebhookEvent(
                "stripe." + event.getType(),
                paymentId,
                intent.getId(),
                amount,
                intent.getCurrency() == null ? null : intent.getCurrency().toUpperCase(),
                success, errorCode, errorReason);
    }

    private static WebhookEvent errorEvent(String errorCode, String reason) {
        return new WebhookEvent("stripe.error", null, null, null, null, false, errorCode, reason);
    }

    private static long toMinorUnits(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new PaymentException(PaymentErrorCode.INVALID_ARGUMENT, "amount is required");
        }
        if (ZERO_DECIMAL_CURRENCIES.contains(currency.toUpperCase())) {
            return amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private static BigDecimal fromMinorUnits(long minor, String currency) {
        if (currency != null && ZERO_DECIMAL_CURRENCIES.contains(currency.toUpperCase())) {
            return BigDecimal.valueOf(minor);
        }
        return BigDecimal.valueOf(minor, 2);
    }
}
