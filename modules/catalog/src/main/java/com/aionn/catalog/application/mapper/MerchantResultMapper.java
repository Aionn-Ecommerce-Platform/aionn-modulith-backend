package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.domain.model.Merchant;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MerchantResultMapper {

    MerchantResult toResult(Merchant merchant);

    List<MerchantResult> toResults(List<Merchant> merchants);

    default PageResult<MerchantResult> toPageResult(PageResult<Merchant> page) {
        return new PageResult<>(
                toResults(page.content()),
                page.page(),
                page.size(),
                page.totalElements());
    }
}
