package com.aionn.payment.application.usecase.payout;

import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.port.in.payout.CompletePayoutInputPort;
import com.aionn.payment.application.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompletePayoutUseCase implements CompletePayoutInputPort {

    private final PayoutService payoutService;

    @Override
    public MerchantPayoutResult execute(String payoutId, String externalRef) {
        return payoutService.markCompleted(payoutId, externalRef);
    }
}
