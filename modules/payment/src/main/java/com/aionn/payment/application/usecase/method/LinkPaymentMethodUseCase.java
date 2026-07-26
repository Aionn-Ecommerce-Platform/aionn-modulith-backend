package com.aionn.payment.application.usecase.method;

import com.aionn.payment.application.dto.method.command.LinkMethodCommand;
import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.mapper.PaymentResultMapper;
import com.aionn.payment.application.port.in.method.LinkPaymentMethodInputPort;
import com.aionn.payment.application.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkPaymentMethodUseCase implements LinkPaymentMethodInputPort {

    private final PaymentMethodService paymentMethodService;
    private final PaymentResultMapper paymentResultMapper;

    @Override
    @Transactional
    public PaymentMethodResult execute(LinkMethodCommand command) {
        return paymentResultMapper.toResult(paymentMethodService.link(command));
    }
}
