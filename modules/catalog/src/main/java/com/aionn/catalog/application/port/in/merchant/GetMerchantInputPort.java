package com.aionn.catalog.application.port.in.merchant;

import com.aionn.catalog.application.dto.merchant.query.GetMerchantQuery;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;

public interface GetMerchantInputPort {

    MerchantResult execute(GetMerchantQuery query);
}
