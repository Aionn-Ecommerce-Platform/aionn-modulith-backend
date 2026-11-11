package com.aionn.catalog.domain.util;

import java.text.Normalizer;
import java.util.Locale;

/** Deterministic, language-independent URL slug used for public product links. */
public final class ProductSlug {

    private static final int MAX_LENGTH = 250;

    private ProductSlug() {
    }

    public static String generate(String name) {
        if (name == null || name.isBlank()) {
            return "san-pham";
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            return "san-pham";
        }
        return normalized.length() > MAX_LENGTH
                ? normalized.substring(0, MAX_LENGTH).replaceAll("-$", "")
                : normalized;
    }
}
