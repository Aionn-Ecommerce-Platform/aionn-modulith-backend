package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.merchant.query.ListMerchantsQuery;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.port.in.merchant.ListMerchantsInputPort;
import com.aionn.catalog.application.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListMerchantsUseCase implements ListMerchantsInputPort {

    private final MerchantService merchantService;

    @Override
    public PageResult<MerchantResult> execute(ListMerchantsQuery query) {
        return merchantService.list(query.pagination());
    }
}
