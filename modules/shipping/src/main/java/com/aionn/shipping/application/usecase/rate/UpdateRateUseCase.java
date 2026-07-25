package com.aionn.shipping.application.usecase.rate;

import com.aionn.shipping.application.dto.rate.command.UpdateRateCommand;
import com.aionn.shipping.application.dto.rate.result.ShippingRateResult;
import com.aionn.shipping.application.mapper.ShippingRateResultMapper;
import com.aionn.shipping.application.port.in.rate.UpdateRateInputPort;
import com.aionn.shipping.application.service.ShippingRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateRateUseCase implements UpdateRateInputPort {

    private final ShippingRateService rateService;
    private final ShippingRateResultMapper shippingRateResultMapper;

    @Override
    @Transactional
    public ShippingRateResult execute(UpdateRateCommand command) {
        return shippingRateResultMapper.toResult(rateService.update(command));
    }
}
