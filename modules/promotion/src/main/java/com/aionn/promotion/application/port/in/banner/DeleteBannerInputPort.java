package com.aionn.promotion.application.port.in.banner;

import com.aionn.promotion.application.dto.banner.command.BannerCommands;

public interface DeleteBannerInputPort {
    void execute(BannerCommands.DeleteBanner command);
}
