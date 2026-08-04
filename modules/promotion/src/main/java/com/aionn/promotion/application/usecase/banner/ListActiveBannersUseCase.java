package com.aionn.promotion.application.usecase.banner;

import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import com.aionn.promotion.application.dto.common.PageResult;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import com.aionn.promotion.application.mapper.PromotionBannerResultMapper;
import com.aionn.promotion.application.port.in.banner.ListActiveBannersInputPort;
import com.aionn.promotion.application.service.PromotionBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListActiveBannersUseCase implements ListActiveBannersInputPort {

    private final PromotionBannerService promotionBannerService;
    private final PromotionBannerResultMapper promotionBannerResultMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<PromotionBannerResult> execute(OffsetPagination pagination) {
        var page = promotionBannerService.listActive(pagination);
        return new PageResult<>(promotionBannerResultMapper.toResults(page.content()),
                page.page(), page.size(), page.totalElements());
    }
}
