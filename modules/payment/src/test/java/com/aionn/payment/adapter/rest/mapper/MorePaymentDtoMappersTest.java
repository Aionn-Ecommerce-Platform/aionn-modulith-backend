package com.aionn.payment.adapter.rest.mapper;

import com.aionn.payment.adapter.rest.dto.method.request.LinkMethodRequest;
import com.aionn.payment.adapter.rest.dto.method.response.PaymentMethodResponse;
import com.aionn.payment.adapter.rest.dto.preference.response.PaymentPreferenceResponse;
import com.aionn.payment.adapter.rest.mapper.method.PaymentMethodDtoMapper;
import com.aionn.payment.adapter.rest.mapper.preference.PaymentPreferenceDtoMapper;
import com.aionn.payment.application.dto.method.command.LinkMethodCommand;
import com.aionn.payment.application.dto.preference.result.PaymentPreferenceResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MorePaymentDtoMappersTest {

    private final PaymentMethodDtoMapper methodMapper = Mappers.getMapper(PaymentMethodDtoMapper.class);
    private final PaymentPreferenceDtoMapper preferenceMapper = Mappers.getMapper(PaymentPreferenceDtoMapper.class);

    @Test
    void paymentMethodDtoMapperShouldMapLinkRequestToCommand() {
        LinkMethodRequest request = new LinkMethodRequest("STRIPE", "4242", "tok-1");
        LinkMethodCommand command = methodMapper.toCommand("user-1", request);

        assertNotNull(command);
        assertEquals("user-1", command.userId());
        assertEquals("tok-1", command.gatewayToken());
    }

    @Test
    void paymentPreferenceDtoMapperShouldMapResultToResponse() {
        PaymentPreferenceResult result = new PaymentPreferenceResult("CARD", "pm-1");
        PaymentPreferenceResponse response = preferenceMapper.toResponse(result);

        assertNotNull(response);
        assertEquals("CARD", response.paymentType());
        assertEquals("pm-1", response.paymentMethodId());
    }
}
