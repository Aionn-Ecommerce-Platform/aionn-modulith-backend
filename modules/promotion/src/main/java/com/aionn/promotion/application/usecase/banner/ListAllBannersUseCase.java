package com.aionn.promotion.application.usecase.banner;

import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import com.aionn.promotion.application.mapper.PromotionBannerResultMapper;
import com.aionn.promotion.application.port.in.banner.ListAllBannersInputPort;
import com.aionn.promotion.application.service.PromotionBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListAllBannersUseCase implements ListAllBannersInputPort {

    private final PromotionBannerService promotionBannerService;
    private final PromotionBannerResultMapper promotionBannerResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PromotionBannerResult> execute() {
        return promotionBannerResultMapper.toResults(promotionBannerService.listAll());
    }
}
