package com.aionn.payment.application.usecase.payout;

import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.port.in.payout.FailPayoutInputPort;
import com.aionn.payment.application.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FailPayoutUseCase implements FailPayoutInputPort {

    private final PayoutService payoutService;

    @Override
    public MerchantPayoutResult execute(String payoutId, String reason) {
        return payoutService.markFailed(payoutId, reason);
    }
}
