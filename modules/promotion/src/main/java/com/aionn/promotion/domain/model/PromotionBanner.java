package com.aionn.promotion.domain.model;

import com.aionn.promotion.domain.exception.PromotionErrorCode;
import com.aionn.promotion.domain.exception.PromotionException;
import lombok.Getter;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

@Getter
public class PromotionBanner {

    private final String bannerId;
    private String title;
    private String imageUrl;
    private String imagePublicId;
    private String linkUrl;
    private int displayOrder;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public PromotionBanner(String bannerId, String title, String imageUrl, String imagePublicId, String linkUrl,
            int displayOrder, boolean active, Instant createdAt, Instant updatedAt) {
        validateImageUrl(imageUrl);
        validateImagePublicId(imagePublicId);
        validateLinkUrl(linkUrl);
        this.bannerId = bannerId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.imagePublicId = imagePublicId;
        this.linkUrl = normalizeLinkUrl(linkUrl);
        this.displayOrder = displayOrder;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PromotionBanner create(String bannerId, String title, String imageUrl, String imagePublicId,
            String linkUrl, int displayOrder, boolean active) {
        return new PromotionBanner(bannerId, title, imageUrl, imagePublicId, linkUrl, displayOrder, active,
                null, null);
    }

    public void update(String title, String imageUrl, String imagePublicId, String linkUrl, Integer displayOrder,
            Boolean active) {
        if (title != null) {
            this.title = title;
        }
        if (imageUrl != null || imagePublicId != null) {
            validateImageUrl(imageUrl);
            validateImagePublicId(imagePublicId);
            this.imageUrl = imageUrl;
            this.imagePublicId = imagePublicId;
        }
        if (linkUrl != null) {
            validateLinkUrl(linkUrl);
            this.linkUrl = normalizeLinkUrl(linkUrl);
        }
        if (displayOrder != null) {
            this.displayOrder = displayOrder;
        }
        if (active != null) {
            this.active = active;
        }
    }

    private static void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new PromotionException(PromotionErrorCode.BANNER_IMAGE_URL_INVALID);
        }
        try {
            URI uri = new URI(imageUrl.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getHost().isBlank()) {
                throw new PromotionException(PromotionErrorCode.BANNER_IMAGE_URL_INVALID);
            }
        } catch (URISyntaxException ex) {
            throw new PromotionException(PromotionErrorCode.BANNER_IMAGE_URL_INVALID);
        }
    }

    private static void validateImagePublicId(String imagePublicId) {
        if (imagePublicId == null || imagePublicId.isBlank()) {
            throw new PromotionException(PromotionErrorCode.BANNER_IMAGE_PUBLIC_ID_INVALID);
        }
    }

    private static void validateLinkUrl(String linkUrl) {
        if (linkUrl == null || linkUrl.isBlank()) {
            return;
        }
        String normalized = linkUrl.trim();
        if (normalized.startsWith("/") && !normalized.startsWith("//") && !normalized.contains("\\")) {
            return;
        }
        try {
            URI uri = new URI(normalized);
            boolean httpScheme = "http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme());
            if (!httpScheme || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new PromotionException(PromotionErrorCode.BANNER_LINK_URL_INVALID);
            }
        } catch (URISyntaxException ex) {
            throw new PromotionException(PromotionErrorCode.BANNER_LINK_URL_INVALID);
        }
    }

    private static String normalizeLinkUrl(String linkUrl) {
        return linkUrl == null || linkUrl.isBlank() ? null : linkUrl.trim();
    }
}
