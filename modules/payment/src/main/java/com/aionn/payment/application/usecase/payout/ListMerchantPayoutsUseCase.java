package com.aionn.payment.application.usecase.payout;

import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.port.in.payout.ListMerchantPayoutsInputPort;
import com.aionn.payment.application.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMerchantPayoutsUseCase implements ListMerchantPayoutsInputPort {

    private final PayoutService payoutService;

    @Override
    public List<MerchantPayoutResult> execute(String ownerId, int limit) {
        return payoutService.listForOwner(ownerId, limit);
    }
}
