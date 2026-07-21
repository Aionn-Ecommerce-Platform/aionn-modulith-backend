package com.aionn.payment.application.port.in.payout;

import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;

import java.util.List;

public interface ListMerchantPayoutsInputPort {
    List<MerchantPayoutResult> execute(String ownerId, int limit);
}
