package com.aionn.promotion.application.usecase.banner;

import com.aionn.promotion.application.dto.banner.command.BannerCommands;
import com.aionn.promotion.application.port.in.banner.DeleteBannerInputPort;
import com.aionn.promotion.application.service.PromotionBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteBannerUseCase implements DeleteBannerInputPort {

    private final PromotionBannerService promotionBannerService;

    @Override
    @Transactional
    public void execute(BannerCommands.DeleteBanner command) {
        promotionBannerService.delete(command);
    }
}
