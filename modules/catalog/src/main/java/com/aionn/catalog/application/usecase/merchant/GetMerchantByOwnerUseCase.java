package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.merchant.query.GetMerchantByOwnerQuery;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.mapper.MerchantResultMapper;
import com.aionn.catalog.application.port.in.merchant.GetMerchantByOwnerInputPort;
import com.aionn.catalog.application.service.MerchantService;
import com.aionn.catalog.domain.model.Merchant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMerchantByOwnerUseCase implements GetMerchantByOwnerInputPort {

    private final MerchantService merchantService;
    private final MerchantResultMapper merchantResultMapper;

    @Override
    @Transactional(readOnly = true)
    public MerchantResult execute(GetMerchantByOwnerQuery query) {
        Merchant merchant = merchantService.getByOwner(query.ownerId());
        return merchantResultMapper.toResult(merchant);
    }
}
