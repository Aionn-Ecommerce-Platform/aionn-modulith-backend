package com.aionn.catalog.application.port.in.merchant;

import com.aionn.catalog.application.dto.merchant.command.UpdateMerchantProfileCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;

public interface UpdateMerchantProfileInputPort {

    MerchantResult execute(UpdateMerchantProfileCommand command);
}
