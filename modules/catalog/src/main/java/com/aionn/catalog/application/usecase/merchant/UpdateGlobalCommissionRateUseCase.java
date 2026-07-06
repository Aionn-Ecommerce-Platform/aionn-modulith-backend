package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.merchant.command.UpdateGlobalCommissionRateCommand;
import com.aionn.catalog.application.port.in.merchant.UpdateGlobalCommissionRateInputPort;
import com.aionn.catalog.application.port.out.settings.CatalogSettingsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateGlobalCommissionRateUseCase implements UpdateGlobalCommissionRateInputPort {

    private final CatalogSettingsPort catalogSettingsPort;

    @Override
    public void execute(UpdateGlobalCommissionRateCommand command) {
        catalogSettingsPort.updateDefaultCommissionRate(command.defaultCommissionRate());
    }
}
