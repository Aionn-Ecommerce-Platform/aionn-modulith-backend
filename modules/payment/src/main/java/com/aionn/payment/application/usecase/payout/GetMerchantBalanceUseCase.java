package com.aionn.payment.application.usecase.payout;

import com.aionn.payment.application.dto.payout.result.MerchantBalanceResult;
import com.aionn.payment.application.port.in.payout.GetMerchantBalanceInputPort;
import com.aionn.payment.application.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMerchantBalanceUseCase implements GetMerchantBalanceInputPort {

    private final PayoutService payoutService;

    @Override
    public MerchantBalanceResult execute(String ownerId, String currency) {
        return payoutService.getBalanceForOwner(ownerId, currency);
    }
}
