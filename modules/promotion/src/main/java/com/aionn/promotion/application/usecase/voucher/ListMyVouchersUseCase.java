package com.aionn.promotion.application.usecase.voucher;

import com.aionn.promotion.application.dto.voucher.result.UserVoucherResult;
import com.aionn.promotion.application.port.in.voucher.ListMyVouchersInputPort;
import com.aionn.promotion.application.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListMyVouchersUseCase implements ListMyVouchersInputPort {

    private final VoucherService voucherService;

    @Override
    @Transactional(readOnly = true)
    public List<UserVoucherResult> execute(String userId, int limit) {
        return voucherService.listMine(userId, limit);
    }
}
