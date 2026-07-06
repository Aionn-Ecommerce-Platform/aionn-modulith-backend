package com.aionn.catalog.application.port.in.merchant;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.merchant.query.ListMerchantsQuery;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;

public interface ListMerchantsInputPort {

    PageResult<MerchantResult> execute(ListMerchantsQuery query);
}
