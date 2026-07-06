package com.aionn.catalog.application.port.in.merchant;

import com.aionn.catalog.application.dto.merchant.command.SuspendMerchantCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;

public interface SuspendMerchantInputPort {

    MerchantResult execute(SuspendMerchantCommand command);
}
