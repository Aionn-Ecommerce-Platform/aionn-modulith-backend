package com.aionn.payment.adapter.rest.mapper.preference;

import com.aionn.payment.adapter.rest.dto.preference.response.PaymentPreferenceResponse;
import com.aionn.payment.application.dto.preference.result.PaymentPreferenceResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentPreferenceDtoMapper {

    PaymentPreferenceResponse toResponse(PaymentPreferenceResult result);
}
