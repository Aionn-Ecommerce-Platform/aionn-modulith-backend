package com.aionn.promotion.application.dto.banner.command;

import com.aionn.sharedkernel.application.command.Command;

public final class BannerCommands {

    private BannerCommands() {
    }

    public record CreateBanner(
            String title,
            String imageUrl,
            String linkUrl,
            int displayOrder,
            boolean active) implements Command {
    }

    public record UpdateBanner(
            String bannerId,
            String title,
            String imageUrl,
            String linkUrl,
            Integer displayOrder,
            Boolean active) implements Command {
    }

    public record DeleteBanner(String bannerId) implements Command {
    }
}
