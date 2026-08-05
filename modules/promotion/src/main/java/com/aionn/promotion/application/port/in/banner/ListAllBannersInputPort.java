package com.aionn.promotion.application.port.in.banner;

import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import com.aionn.promotion.application.dto.common.PageResult;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;

public interface ListAllBannersInputPort {
    PageResult<PromotionBannerResult> execute(OffsetPagination pagination);
}
