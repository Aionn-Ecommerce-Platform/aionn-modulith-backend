package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.merchant.command.CloseMerchantCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.port.in.merchant.CloseMerchantInputPort;
import com.aionn.catalog.application.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CloseMerchantUseCase implements CloseMerchantInputPort {

    private final MerchantService merchantService;

    @Override
    public MerchantResult execute(CloseMerchantCommand command) {
        return merchantService.close(command);
    }
}
