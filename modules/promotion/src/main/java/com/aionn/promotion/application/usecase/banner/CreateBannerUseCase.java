package com.aionn.promotion.application.usecase.banner;

import com.aionn.promotion.application.dto.banner.command.BannerCommands;
import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import com.aionn.promotion.application.mapper.PromotionBannerResultMapper;
import com.aionn.promotion.application.port.in.banner.CreateBannerInputPort;
import com.aionn.promotion.application.service.PromotionBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateBannerUseCase implements CreateBannerInputPort {

    private final PromotionBannerService promotionBannerService;
    private final PromotionBannerResultMapper promotionBannerResultMapper;

    @Override
    @Transactional
    public PromotionBannerResult execute(BannerCommands.CreateBanner command) {
        return promotionBannerResultMapper.toResult(promotionBannerService.create(command));
    }
}
