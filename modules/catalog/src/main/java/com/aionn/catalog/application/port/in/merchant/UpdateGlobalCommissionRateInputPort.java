package com.aionn.catalog.application.port.in.merchant;

import com.aionn.catalog.application.dto.merchant.command.UpdateGlobalCommissionRateCommand;

public interface UpdateGlobalCommissionRateInputPort {
    void execute(UpdateGlobalCommissionRateCommand command);
}
