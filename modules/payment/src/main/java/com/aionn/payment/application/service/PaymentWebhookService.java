package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.port.in.payment.ConfirmPaymentInputPort;
import com.aionn.payment.application.port.in.payment.FailPaymentInputPort;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private final PaymentProviderRouter providerRouter;
    private final ConfirmPaymentInputPort confirmPaymentInputPort;
    private final FailPaymentInputPort failPaymentInputPort;

    public boolean handle(String gateway, byte[] rawBody, String signature) {
        PaymentGatewayKind kind;
        try {
            kind = PaymentGatewayKind.valueOf(gateway.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown gateway in webhook URL: {}", gateway);
            return false;
        }

        PaymentProviderClient.WebhookEvent event = providerRouter.route(kind)
                .verifyAndParse(new String(rawBody, StandardCharsets.UTF_8), signature);
        if (event.paymentId() == null) {
            if (isIgnorableEvent(kind, event)) {
                return true;
            }
            log.warn("Webhook without paymentId for gateway {}", kind);
            return false;
        }
        if (event.success()) {
            confirmPaymentInputPort.execute(new ConfirmPaymentCommand(
                    event.paymentId(), event.transactionNo(), event.amount(), event.currency()));
        } else {
            failPaymentInputPort.execute(new FailPaymentCommand(
                    event.paymentId(),
                    event.errorCode() == null ? kind.defaultFailureCode() : event.errorCode(),
                    event.errorReason()));
        }
        return true;
    }

    private static boolean isIgnorableEvent(PaymentGatewayKind kind, PaymentProviderClient.WebhookEvent event) {
        return kind == PaymentGatewayKind.STRIPE
                && event.errorCode() == null
                && event.type() != null
                && event.type().startsWith("stripe.");
    }
}
