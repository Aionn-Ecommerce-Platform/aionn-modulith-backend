package com.aionn.payment.application.port.in.payout;

import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;

import java.util.List;

public interface ListPayoutsByStatusInputPort {
    List<MerchantPayoutResult> execute(String status, int limit);
}
