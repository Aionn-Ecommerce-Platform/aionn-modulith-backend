package com.aionn.shipping.application.port.in.rate;

import com.aionn.shipping.application.dto.rate.command.ConfigureRateCommand;
import com.aionn.shipping.application.dto.rate.result.ShippingRateResult;

public interface ConfigureRateInputPort {
    ShippingRateResult execute(ConfigureRateCommand command);
}
