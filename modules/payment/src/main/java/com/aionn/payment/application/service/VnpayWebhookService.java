package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.in.payment.ConfirmPaymentInputPort;
import com.aionn.payment.application.port.in.payment.FailPaymentInputPort;
import com.aionn.payment.application.port.in.payment.GetPaymentInputPort;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import com.aionn.payment.application.port.out.PaymentProviderRouter;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.valueobject.PaymentGatewayKind;
import com.aionn.payment.domain.valueobject.VnpayResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnpayWebhookService {

    private final PaymentProviderRouter providerRouter;
    private final GetPaymentInputPort getPaymentInputPort;
    private final ConfirmPaymentInputPort confirmPaymentInputPort;
    private final FailPaymentInputPort failPaymentInputPort;

    public ReturnResult handleReturn(String query) {
        PaymentProviderClient.WebhookEvent event = verify(query);
        if (event.paymentId() == null) {
            log.warn("VNPay return without paymentId, query={}", query);
            return new ReturnResult(ReturnStatus.BAD_REQUEST, null, null);
        }
        try {
            finalise(event);
        } catch (RuntimeException ex) {
            log.error("VNPay return finalisation failed for {}", event.paymentId(), ex);
            return new ReturnResult(ReturnStatus.INTERNAL_ERROR, event.paymentId(), null);
        }
        PaymentResult payment = getPaymentInputPort.execute(event.paymentId());
        return new ReturnResult(ReturnStatus.SUCCESS, payment.paymentId(), payment.orderId());
    }

    public IpnResult handleIpn(String query) {
        PaymentProviderClient.WebhookEvent event = verify(query);
        if (event.paymentId() == null) {
            return new IpnResult(VnpayResponseCode.INVALID_SIGNATURE);
        }
        try {
            finalise(event);
            return new IpnResult(VnpayResponseCode.CONFIRM_SUCCESS);
        } catch (PaymentException ex) {
            if (PaymentErrorCode.PAYMENT_NOT_FOUND.getCode().equals(ex.getErrorCode())) {
                return new IpnResult(VnpayResponseCode.ORDER_NOT_FOUND);
            }
            return new IpnResult(VnpayResponseCode.UNKNOWN_ERROR);
        }
    }

    private PaymentProviderClient.WebhookEvent verify(String query) {
        return providerRouter.route(PaymentGatewayKind.VNPAY).verifyAndParse(query, null);
    }

    private void finalise(PaymentProviderClient.WebhookEvent event) {
        if (event.success()) {
            confirmPaymentInputPort.execute(new ConfirmPaymentCommand(
                    event.paymentId(), event.transactionNo(), event.amount(), event.currency()));
        } else {
            failPaymentInputPort.execute(new FailPaymentCommand(
                    event.paymentId(),
                    event.errorCode() == null
                            ? PaymentGatewayKind.VNPAY.defaultFailureCode()
                            : event.errorCode(),
                    event.errorReason()));
        }
    }

    public enum ReturnStatus {
        SUCCESS,
        BAD_REQUEST,
        INTERNAL_ERROR
    }

    public record ReturnResult(ReturnStatus status, String paymentId, String orderId) {
    }

    public record IpnResult(VnpayResponseCode code) {
    }
}
