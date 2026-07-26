package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.merchant.command.CloseMerchantCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.mapper.MerchantResultMapper;
import com.aionn.catalog.application.port.in.merchant.CloseMerchantInputPort;
import com.aionn.catalog.application.service.MerchantService;
import com.aionn.catalog.domain.model.Merchant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CloseMerchantUseCase implements CloseMerchantInputPort {

    private final MerchantService merchantService;
    private final MerchantResultMapper merchantResultMapper;

    @Override
    @Transactional
    public MerchantResult execute(CloseMerchantCommand command) {
        Merchant merchant = merchantService.close(command);
        return merchantResultMapper.toResult(merchant);
    }
}
