package com.aionn.payment.application.usecase.method;

import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.mapper.PaymentResultMapper;
import com.aionn.payment.application.port.in.method.ListPaymentMethodsInputPort;
import com.aionn.payment.application.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPaymentMethodsUseCase implements ListPaymentMethodsInputPort {

    private final PaymentMethodService paymentMethodService;
    private final PaymentResultMapper paymentResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodResult> execute(String userId) {
        return paymentMethodService.listMine(userId).stream()
                .map(paymentResultMapper::toResult)
                .toList();
    }
}
