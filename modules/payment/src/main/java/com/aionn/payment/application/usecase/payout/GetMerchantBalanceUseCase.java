package com.aionn.payment.application.usecase.payout;

import com.aionn.payment.application.dto.payout.result.MerchantBalanceResult;
import com.aionn.payment.application.mapper.PayoutResultMapper;
import com.aionn.payment.application.port.in.payout.GetMerchantBalanceInputPort;
import com.aionn.payment.application.policy.PaymentDefaultsPolicy;
import com.aionn.payment.application.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMerchantBalanceUseCase implements GetMerchantBalanceInputPort {

    private final PayoutService payoutService;
    private final PayoutResultMapper payoutResultMapper;
    private final PaymentDefaultsPolicy defaultsPolicy;

    @Override
    @Transactional(readOnly = true)
    public MerchantBalanceResult execute(String ownerId, String currency) {
        String resolvedCurrency = currency == null || currency.isBlank()
                ? defaultsPolicy.defaultCurrency()
                : currency;
        return payoutResultMapper.toResult(payoutService.getBalanceForOwner(ownerId, resolvedCurrency));
    }
}
