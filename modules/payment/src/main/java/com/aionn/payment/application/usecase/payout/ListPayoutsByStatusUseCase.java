package com.aionn.payment.application.usecase.payout;

import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.port.in.payout.ListPayoutsByStatusInputPort;
import com.aionn.payment.application.service.PayoutService;
import com.aionn.payment.domain.valueobject.PayoutStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPayoutsByStatusUseCase implements ListPayoutsByStatusInputPort {

    private final PayoutService payoutService;

    @Override
    public List<MerchantPayoutResult> execute(String status, int limit) {
        return payoutService.listByStatus(PayoutStatus.valueOf(status), limit);
    }
}
