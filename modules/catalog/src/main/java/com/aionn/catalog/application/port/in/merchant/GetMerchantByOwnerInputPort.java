package com.aionn.catalog.application.port.in.merchant;

import com.aionn.catalog.application.dto.merchant.query.GetMerchantByOwnerQuery;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;

public interface GetMerchantByOwnerInputPort {

    MerchantResult execute(GetMerchantByOwnerQuery query);
}
