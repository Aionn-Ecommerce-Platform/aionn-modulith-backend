package com.aionn.promotion.application.usecase.banner;

import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import com.aionn.promotion.application.mapper.PromotionBannerResultMapper;
import com.aionn.promotion.application.port.in.banner.ListActiveBannersInputPort;
import com.aionn.promotion.application.service.PromotionBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListActiveBannersUseCase implements ListActiveBannersInputPort {

    private final PromotionBannerService promotionBannerService;
    private final PromotionBannerResultMapper promotionBannerResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PromotionBannerResult> execute() {
        return promotionBannerResultMapper.toResults(promotionBannerService.listActive());
    }
}
