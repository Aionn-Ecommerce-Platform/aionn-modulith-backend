package com.aionn.promotion.application.usecase.voucher;

import com.aionn.promotion.application.dto.voucher.result.VoucherSummaryResult;
import com.aionn.promotion.application.port.in.voucher.GetVoucherSummaryInputPort;
import com.aionn.promotion.application.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetVoucherSummaryUseCase implements GetVoucherSummaryInputPort {

    private final VoucherService voucherService;

    @Override
    @Transactional(readOnly = true)
    public VoucherSummaryResult execute() {
        var summary = voucherService.countAvailable();
        return new VoucherSummaryResult(
                summary.platform() + summary.shop() + summary.shipping(),
                summary.platform(),
                summary.shop(),
                summary.shipping());
    }
}
