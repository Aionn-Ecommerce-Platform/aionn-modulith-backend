package com.aionn.shipping.application.port.in.rate;

import com.aionn.shipping.application.dto.rate.command.UpdateRateCommand;
import com.aionn.shipping.application.dto.rate.result.ShippingRateResult;

public interface UpdateRateInputPort {
    ShippingRateResult execute(UpdateRateCommand command);
}
