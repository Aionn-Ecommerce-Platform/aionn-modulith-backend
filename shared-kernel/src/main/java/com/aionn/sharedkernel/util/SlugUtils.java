package com.aionn.sharedkernel.util;

import java.text.Normalizer;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank())
            return "";

        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);

        return normalized
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[\u0111\u0110]", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    public static String toProductSlug(String name, String sku) {
        return slugify(name) + "-" + slugify(sku);
    }

    public static String uniqueSlug(String base, String uniqueSuffix) {
        return slugify(base) + "-" + uniqueSuffix.toLowerCase();
    }

    public static boolean isValidSlug(String slug) {
        if (slug == null || slug.isBlank())
            return false;
        return slug.matches("^[a-z0-9]+(-[a-z0-9]+)*$");
    }
}
