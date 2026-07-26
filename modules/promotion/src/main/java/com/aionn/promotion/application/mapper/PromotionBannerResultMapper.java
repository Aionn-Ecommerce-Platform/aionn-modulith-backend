package com.aionn.promotion.application.mapper;

import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import com.aionn.promotion.domain.model.PromotionBanner;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PromotionBannerResultMapper {

    PromotionBannerResult toResult(PromotionBanner banner);

    List<PromotionBannerResult> toResults(List<PromotionBanner> banners);
}
