package com.aionn.payment.application.port.in.payout;

import com.aionn.payment.application.dto.payout.result.MerchantBalanceResult;

public interface GetMerchantBalanceInputPort {
    MerchantBalanceResult execute(String ownerId, String currency);
}
