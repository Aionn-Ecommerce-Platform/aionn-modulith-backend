package com.aionn.catalog.application.port.in.merchant;

import com.aionn.catalog.application.dto.merchant.command.ActivateMerchantCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;

public interface ActivateMerchantInputPort {

    MerchantResult execute(ActivateMerchantCommand command);
}
