package com.aionn.promotion.application.port.in.banner;

import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;

public interface GetBannerInputPort {
    PromotionBannerResult execute(String bannerId);
}
