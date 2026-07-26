package com.aionn.promotion.application.port.in.banner;

import com.aionn.promotion.application.dto.banner.command.BannerCommands;
import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;

public interface UpdateBannerInputPort {
    PromotionBannerResult execute(BannerCommands.UpdateBanner command);
}
