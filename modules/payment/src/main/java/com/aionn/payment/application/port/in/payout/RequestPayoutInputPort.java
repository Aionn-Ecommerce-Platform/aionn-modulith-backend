package com.aionn.payment.application.port.in.payout;

import com.aionn.payment.application.dto.payout.command.RequestPayoutCommand;
import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;

public interface RequestPayoutInputPort {
    MerchantPayoutResult execute(RequestPayoutCommand command);
}
