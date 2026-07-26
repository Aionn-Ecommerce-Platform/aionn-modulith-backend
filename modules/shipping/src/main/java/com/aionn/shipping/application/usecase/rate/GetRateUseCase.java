package com.aionn.shipping.application.usecase.rate;

import com.aionn.shipping.application.dto.rate.result.ShippingRateResult;
import com.aionn.shipping.application.mapper.ShippingRateResultMapper;
import com.aionn.shipping.application.port.in.rate.GetRateInputPort;
import com.aionn.shipping.application.service.ShippingRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRateUseCase implements GetRateInputPort {

    private final ShippingRateService rateService;
    private final ShippingRateResultMapper shippingRateResultMapper;

    @Override
    @Transactional(readOnly = true)
    public ShippingRateResult execute(String rateId) {
        return shippingRateResultMapper.toResult(rateService.get(rateId));
    }
}
