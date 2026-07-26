package com.aionn.promotion.application.port.in.banner;

import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;

import java.util.List;

public interface ListActiveBannersInputPort {
    List<PromotionBannerResult> execute();
}
