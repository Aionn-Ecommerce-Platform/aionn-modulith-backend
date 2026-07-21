package com.aionn.payment.adapter.rest.mapper.payout;

import com.aionn.payment.adapter.rest.dto.payout.request.PayoutRequestBody;
import com.aionn.payment.adapter.rest.dto.payout.response.MerchantBalanceResponse;
import com.aionn.payment.adapter.rest.dto.payout.response.PayoutResponse;
import com.aionn.payment.application.dto.payout.command.RequestPayoutCommand;
import com.aionn.payment.application.dto.payout.result.MerchantBalanceResult;
import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PayoutDtoMapper {

    MerchantBalanceResponse toResponse(MerchantBalanceResult result);

    PayoutResponse toResponse(MerchantPayoutResult result);

    List<PayoutResponse> toResponses(List<MerchantPayoutResult> results);

    @Mapping(target = "currency", expression = "java(request.currency() == null ? \"VND\" : request.currency())")
    RequestPayoutCommand toCommand(String ownerId, PayoutRequestBody request);
}
