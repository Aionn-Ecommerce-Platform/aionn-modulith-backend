package com.aionn.payment.application.usecase.payout;

import com.aionn.payment.application.dto.payout.command.RequestPayoutCommand;
import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.port.in.payout.RequestPayoutInputPort;
import com.aionn.payment.application.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestPayoutUseCase implements RequestPayoutInputPort {

    private final PayoutService payoutService;

    @Override
    public MerchantPayoutResult execute(RequestPayoutCommand command) {
        return payoutService.requestPayout(
                command.ownerId(), command.amount(), command.currency(),
                command.bankName(), command.bankAccountNo(), command.bankAccountName(), command.note());
    }
}
