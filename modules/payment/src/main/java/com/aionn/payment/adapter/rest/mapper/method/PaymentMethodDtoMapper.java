package com.aionn.payment.adapter.rest.mapper.method;

import com.aionn.payment.adapter.rest.dto.method.request.LinkMethodRequest;
import com.aionn.payment.adapter.rest.dto.method.response.PaymentMethodResponse;
import com.aionn.payment.adapter.rest.dto.method.response.StripeSetupIntentResponse;
import com.aionn.payment.application.dto.method.command.LinkMethodCommand;
import com.aionn.payment.application.dto.method.command.RemoveMethodCommand;
import com.aionn.payment.application.dto.method.command.VerifyMethodCommand;
import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.dto.method.result.StripeSetupIntentResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMethodDtoMapper {

    LinkMethodCommand toCommand(String userId, LinkMethodRequest request);

    VerifyMethodCommand toVerifyCommand(String userId, String methodId);

    RemoveMethodCommand toRemoveCommand(String userId, String methodId);

    PaymentMethodResponse toResponse(PaymentMethodResult result);

    List<PaymentMethodResponse> toResponses(List<PaymentMethodResult> results);

    StripeSetupIntentResponse toResponse(StripeSetupIntentResult result);
}
