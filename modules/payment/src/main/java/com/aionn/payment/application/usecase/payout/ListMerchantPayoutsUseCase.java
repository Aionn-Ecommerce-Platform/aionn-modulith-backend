package com.aionn.payment.application.usecase.payout;

import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.mapper.PayoutResultMapper;
import com.aionn.payment.application.port.in.payout.ListMerchantPayoutsInputPort;
import com.aionn.payment.application.service.PayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMerchantPayoutsUseCase implements ListMerchantPayoutsInputPort {

    private final PayoutService payoutService;
    private final PayoutResultMapper payoutResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MerchantPayoutResult> execute(String ownerId, int limit) {
        return payoutService.listForOwner(ownerId, limit).stream()
                .map(payoutResultMapper::toResult)
                .toList();
    }
}
