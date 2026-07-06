package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.merchant.query.GetMerchantQuery;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.port.in.merchant.GetMerchantInputPort;
import com.aionn.catalog.application.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMerchantUseCase implements GetMerchantInputPort {

    private final MerchantService merchantService;

    @Override
    public MerchantResult execute(GetMerchantQuery query) {
        return merchantService.get(query.merchantId());
    }
}
