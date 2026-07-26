package com.aionn.payment.application.usecase.payout;

import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.mapper.PayoutResultMapper;
import com.aionn.payment.application.port.in.payout.FailPayoutInputPort;
import com.aionn.payment.application.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FailPayoutUseCase implements FailPayoutInputPort {

    private final PayoutService payoutService;
    private final PayoutResultMapper payoutResultMapper;

    @Override
    @Transactional
    public MerchantPayoutResult execute(String payoutId, String reason) {
        return payoutResultMapper.toResult(payoutService.markFailed(payoutId, reason));
    }
}
