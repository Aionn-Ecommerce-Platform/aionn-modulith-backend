package com.aionn.payment.adapter.rest.mapper.payment;

import com.aionn.payment.adapter.rest.dto.payment.request.InitiatePaymentRequest;
import com.aionn.payment.adapter.rest.dto.payment.request.RefundRequest;
import com.aionn.payment.adapter.rest.dto.payment.response.PaymentResponse;
import com.aionn.payment.application.dto.payment.command.ConfirmPaymentCommand;
import com.aionn.payment.application.dto.payment.command.FailPaymentCommand;
import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payment.command.RefundPaymentCommand;
import com.aionn.payment.application.dto.payment.result.PaymentResult;
import com.aionn.payment.application.port.out.PaymentProviderClient;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentDtoMapper {

    InitiatePaymentCommand toCommand(InitiatePaymentRequest request, String userId, String idempotencyKey);

    RefundPaymentCommand toCommand(String paymentId, RefundRequest request);

    default ConfirmPaymentCommand toConfirmCommand(PaymentProviderClient.WebhookEvent event) {
        return new ConfirmPaymentCommand(event.paymentId(), event.transactionNo());
    }

    default FailPaymentCommand toFailCommand(PaymentProviderClient.WebhookEvent event, String defaultErrorCode) {
        String errorCode = event.errorCode() != null ? event.errorCode() : defaultErrorCode;
        return new FailPaymentCommand(event.paymentId(), errorCode, event.errorReason());
    }

    PaymentResponse toResponse(PaymentResult result);

    List<PaymentResponse> toResponses(List<PaymentResult> results);
}
