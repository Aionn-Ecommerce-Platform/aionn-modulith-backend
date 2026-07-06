package com.aionn.catalog.application.port.in.merchant;

import com.aionn.catalog.application.dto.merchant.command.CloseMerchantCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;

public interface CloseMerchantInputPort {

    MerchantResult execute(CloseMerchantCommand command);
}
