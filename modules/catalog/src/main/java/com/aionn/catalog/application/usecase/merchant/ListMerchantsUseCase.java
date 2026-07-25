package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.merchant.query.ListMerchantsQuery;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.mapper.MerchantResultMapper;
import com.aionn.catalog.application.port.in.merchant.ListMerchantsInputPort;
import com.aionn.catalog.application.service.MerchantService;
import com.aionn.catalog.domain.model.Merchant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListMerchantsUseCase implements ListMerchantsInputPort {

    private final MerchantService merchantService;
    private final MerchantResultMapper merchantResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<MerchantResult> execute(ListMerchantsQuery query) {
        PageResult<Merchant> page = merchantService.list(query.pagination());
        return merchantResultMapper.toPageResult(page);
    }
}
