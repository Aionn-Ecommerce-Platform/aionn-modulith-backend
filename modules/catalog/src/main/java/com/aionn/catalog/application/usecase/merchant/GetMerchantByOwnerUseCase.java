package com.aionn.catalog.application.usecase.merchant;

import com.aionn.catalog.application.dto.merchant.query.GetMerchantByOwnerQuery;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.port.in.merchant.GetMerchantByOwnerInputPort;
import com.aionn.catalog.application.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMerchantByOwnerUseCase implements GetMerchantByOwnerInputPort {

    private final MerchantService merchantService;

    @Override
    public MerchantResult execute(GetMerchantByOwnerQuery query) {
        return merchantService.getByOwner(query.ownerId());
    }
}
