package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.merchant.command.UpdateMerchantCommissionRateCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.port.in.merchant.UpdateMerchantCommissionRateInputPort;
import com.aionn.catalog.application.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateMerchantCommissionRateUseCase implements UpdateMerchantCommissionRateInputPort {

    private final MerchantService merchantService;

    @Override
    public MerchantResult execute(UpdateMerchantCommissionRateCommand command) {
        return merchantService.updateCommissionRate(command);
    }
}
