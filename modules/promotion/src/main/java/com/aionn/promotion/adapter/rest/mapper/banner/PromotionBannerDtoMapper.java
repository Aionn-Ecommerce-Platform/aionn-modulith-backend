package com.aionn.promotion.adapter.rest.mapper.banner;

import com.aionn.promotion.adapter.rest.dto.banner.CreateBannerRequest;
import com.aionn.promotion.adapter.rest.dto.banner.UpdateBannerRequest;
import com.aionn.promotion.adapter.rest.dto.banner.response.PromotionBannerResponse;
import com.aionn.promotion.application.dto.banner.command.BannerCommands;
import com.aionn.promotion.application.dto.banner.result.PromotionBannerResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PromotionBannerDtoMapper {

    PromotionBannerResponse toResponse(PromotionBannerResult result);

    List<PromotionBannerResponse> toResponses(List<PromotionBannerResult> results);

    default BannerCommands.CreateBanner toCreateCommand(CreateBannerRequest request) {
        boolean active = request.active() == null || request.active();
        return new BannerCommands.CreateBanner(request.title(), request.imageUrl(), request.imagePublicId(),
                request.linkUrl(), request.displayOrder(), active);
    }

    default BannerCommands.UpdateBanner toUpdateCommand(String bannerId, UpdateBannerRequest request) {
        return new BannerCommands.UpdateBanner(bannerId, request.title(), request.imageUrl(), request.imagePublicId(),
                request.linkUrl(), request.displayOrder(), request.active());
    }

    default BannerCommands.DeleteBanner toDeleteCommand(String bannerId) {
        return new BannerCommands.DeleteBanner(bannerId);
    }
}
