package com.aionn.payment.application.port.in.payout;

import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;

public interface CompletePayoutInputPort {
    MerchantPayoutResult execute(String payoutId, String externalRef);
}
