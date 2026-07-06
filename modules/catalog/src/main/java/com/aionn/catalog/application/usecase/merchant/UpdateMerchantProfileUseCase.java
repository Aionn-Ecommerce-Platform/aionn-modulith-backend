package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.merchant.command.UpdateMerchantProfileCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.port.in.merchant.UpdateMerchantProfileInputPort;
import com.aionn.catalog.application.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateMerchantProfileUseCase implements UpdateMerchantProfileInputPort {

    private final MerchantService merchantService;

    @Override
    public MerchantResult execute(UpdateMerchantProfileCommand command) {
        return merchantService.updateProfile(command);
    }
}
