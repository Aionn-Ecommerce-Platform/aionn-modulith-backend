package com.aionn.catalog.application.port.in.merchant;

import com.aionn.catalog.application.dto.merchant.command.RegisterMerchantCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;

public interface RegisterMerchantInputPort {

    MerchantResult execute(RegisterMerchantCommand command);
}
