package com.aionn.promotion.domain.model;

import lombok.Getter;

import java.time.Instant;

@Getter
public class PromotionBanner {

    private final String bannerId;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private int displayOrder;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public PromotionBanner(String bannerId, String title, String imageUrl, String linkUrl,
            int displayOrder, boolean active, Instant createdAt, Instant updatedAt) {
        this.bannerId = bannerId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.displayOrder = displayOrder;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PromotionBanner create(String bannerId, String title, String imageUrl,
            String linkUrl, int displayOrder, boolean active) {
        return new PromotionBanner(bannerId, title, imageUrl, linkUrl, displayOrder, active,
                null, null);
    }

    public void update(String title, String imageUrl, String linkUrl, Integer displayOrder,
            Boolean active) {
        if (title != null) {
            this.title = title;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
        if (linkUrl != null) {
            this.linkUrl = linkUrl;
        }
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
        if (active != null) {
            this.active = active;
        }
    }
}
