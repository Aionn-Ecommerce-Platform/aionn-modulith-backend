package com.aionn.payment.adapter.rest.mapper;

import com.aionn.payment.adapter.rest.dto.payment.request.InitiatePaymentRequest;
import com.aionn.payment.adapter.rest.dto.payout.request.PayoutRequestBody;
import com.aionn.payment.adapter.rest.mapper.payment.PaymentDtoMapper;
import com.aionn.payment.adapter.rest.mapper.payout.PayoutDtoMapper;
import com.aionn.payment.application.dto.payment.command.InitiatePaymentCommand;
import com.aionn.payment.application.dto.payout.command.RequestPayoutCommand;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PaymentDtoMappersTest {

    private final PaymentDtoMapper paymentMapper = Mappers.getMapper(PaymentDtoMapper.class);
    private final PayoutDtoMapper payoutMapper = Mappers.getMapper(PayoutDtoMapper.class);

    @Test
    void paymentDtoMapperShouldMapInitiateRequestToCommand() {
        InitiatePaymentRequest request = new InitiatePaymentRequest(
                "order-1", "pm-1", BigDecimal.TEN, "VND", com.aionn.payment.domain.valueobject.PaymentGatewayKind.STRIPE, "key-1");

        InitiatePaymentCommand command = paymentMapper.toCommand(request, "user-1", "key-1");

        assertNotNull(command);
        assertEquals("user-1", command.userId());
        assertEquals("order-1", command.orderId());
        assertEquals(BigDecimal.TEN, command.amount());
    }

    @Test
    void payoutDtoMapperShouldMapRequestPayoutToCommand() {
        PayoutRequestBody request = new PayoutRequestBody(
                BigDecimal.valueOf(100000), "VND", "VCB", "123456", "John", "Monthly payout");

        RequestPayoutCommand command = payoutMapper.toCommand("owner-1", request);

        assertNotNull(command);
        assertEquals("owner-1", command.ownerId());
        assertEquals(BigDecimal.valueOf(100000), command.amount());
        assertEquals("VCB", command.bankName());
    }
}
