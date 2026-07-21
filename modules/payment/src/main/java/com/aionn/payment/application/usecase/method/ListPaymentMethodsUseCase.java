package com.aionn.payment.application.usecase.method;

import com.aionn.payment.application.dto.method.result.PaymentMethodResult;
import com.aionn.payment.application.port.in.method.ListPaymentMethodsInputPort;
import com.aionn.payment.application.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPaymentMethodsUseCase implements ListPaymentMethodsInputPort {

    private final PaymentMethodService paymentMethodService;

    @Override
    public List<PaymentMethodResult> execute(String userId) {
        return paymentMethodService.listMine(userId);
    }
}
