package com.aionn.promotion.application.usecase.banner;

import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import com.aionn.promotion.application.mapper.PromotionBannerResultMapper;
import com.aionn.promotion.application.port.in.banner.GetBannerInputPort;
import com.aionn.promotion.application.service.PromotionBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetBannerUseCase implements GetBannerInputPort {

    private final PromotionBannerService promotionBannerService;
    private final PromotionBannerResultMapper promotionBannerResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PromotionBannerResult execute(String bannerId) {
        return promotionBannerResultMapper.toResult(promotionBannerService.get(bannerId));
    }
}
