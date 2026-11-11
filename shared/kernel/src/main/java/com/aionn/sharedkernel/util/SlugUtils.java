package com.aionn.sharedkernel.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank())
            return "";

        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);

        String slug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('\u0111', 'd')
                .replace('\u0110', 'd');
        slug = slug.replaceAll("[^a-z0-9\\s-]", "").trim();
        slug = slug.replaceAll("\\s+", "-");
        slug = collapseDashes(slug);
        return trimEdgeDashes(slug);
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
        if (slug.charAt(0) == '-' || slug.charAt(slug.length() - 1) == '-') {
            return false;
        }
        boolean previousDash = false;
        for (int i = 0; i < slug.length(); i++) {
            char ch = slug.charAt(i);
            if (ch == '-') {
                if (previousDash) {
                    return false;
                }
                previousDash = true;
                continue;
            }
            if (!Character.isLowerCase(ch) && !Character.isDigit(ch)) {
                return false;
            }
            previousDash = false;
        }
        return true;
    }

    private static String trimEdgeDashes(String slug) {
        int start = 0;
        int end = slug.length();
        while (start < end && slug.charAt(start) == '-') {
            start++;
        }
        while (end > start && slug.charAt(end - 1) == '-') {
            end--;
        }
        return slug.substring(start, end);
    }

    private static String collapseDashes(String slug) {
        StringBuilder builder = new StringBuilder(slug.length());
        boolean previousDash = false;
        for (int i = 0; i < slug.length(); i++) {
            char ch = slug.charAt(i);
            if (ch == '-') {
                if (!previousDash) {
                    builder.append(ch);
                }
                previousDash = true;
                continue;
            }
            builder.append(ch);
            previousDash = false;
        }
        return builder.toString();
    }
}
