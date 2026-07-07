package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.domain.model.Merchant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MerchantResultMapper {

    MerchantResult toResult(Merchant merchant);
}
