package com.aionn.shipping.application.port.in.rate;

import com.aionn.shipping.application.dto.rate.result.ShippingRateResult;

public interface GetRateInputPort {
    ShippingRateResult execute(String rateId);
}
