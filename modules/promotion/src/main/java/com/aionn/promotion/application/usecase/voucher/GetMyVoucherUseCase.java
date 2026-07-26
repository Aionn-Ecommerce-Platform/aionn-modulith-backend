package com.aionn.promotion.application.usecase.voucher;

import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.application.port.in.voucher.GetMyVoucherInputPort;
import com.aionn.promotion.application.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyVoucherUseCase implements GetMyVoucherInputPort {

    private final VoucherService voucherService;

    @Override
    @Transactional(readOnly = true)
    public UserVoucherResult execute(String userId, String voucherCode) {
        return voucherService.getMine(userId, voucherCode);
    }
}
