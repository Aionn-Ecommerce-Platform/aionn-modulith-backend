package com.aionn.payment.application.mapper;

import com.aionn.payment.application.dto.payout.result.MerchantBalanceResult;
import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.domain.model.MerchantPayout;
import org.springframework.stereotype.Component;

@Component
public class PayoutResultMapper {

    public MerchantPayoutResult toResult(MerchantPayout p) {
        return new MerchantPayoutResult(
                p.getPayoutId(), p.getMerchantId(), p.getAmount(), p.getCurrency(),
                p.getStatus().name(), p.getBankName(), p.getBankAccountNo(), p.getBankAccountName(),
                p.getExternalRef(), p.getNote(),
                p.getRequestedAt(), p.getCompletedAt(), p.getFailedAt(), p.getFailureReason());
    }

    public MerchantBalanceResult toResult(MerchantBalance b) {
        return new MerchantBalanceResult(
                b.getMerchantId(), b.getCurrency(), b.getPending(), b.getAvailable(), b.getUpdatedAt());
    }
}
