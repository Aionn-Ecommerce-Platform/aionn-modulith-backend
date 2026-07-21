package com.aionn.payment.application.port.in.payout;

import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;

public interface FailPayoutInputPort {
    MerchantPayoutResult execute(String payoutId, String reason);
}
