package com.aionn.payment.application.usecase.payout;

import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.mapper.PayoutResultMapper;
import com.aionn.payment.application.port.in.payout.ListPayoutsByStatusInputPort;
import com.aionn.payment.application.service.PayoutService;
import com.aionn.payment.domain.valueobject.PayoutStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPayoutsByStatusUseCase implements ListPayoutsByStatusInputPort {

    private final PayoutService payoutService;
    private final PayoutResultMapper payoutResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MerchantPayoutResult> execute(String status, int limit) {
        return payoutService.listByStatus(PayoutStatus.valueOf(status), limit).stream()
                .map(payoutResultMapper::toResult)
                .toList();
    }
}
