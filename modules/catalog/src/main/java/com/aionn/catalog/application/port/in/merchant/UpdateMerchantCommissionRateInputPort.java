package com.aionn.catalog.application.port.in.merchant;

import com.aionn.catalog.application.dto.merchant.command.UpdateMerchantCommissionRateCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;

public interface UpdateMerchantCommissionRateInputPort {
    MerchantResult execute(UpdateMerchantCommissionRateCommand command);
}
