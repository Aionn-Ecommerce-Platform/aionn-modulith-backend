package com.aionn.sharedkernel.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_SLUG_CHARS_PATTERN = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern MULTI_DASH_PATTERN = Pattern.compile("-+");
    private static final Pattern VALID_SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private SlugUtils() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank())
            return "";

        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);

        String slug = DIACRITICS_PATTERN.matcher(normalized).replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replace('\u0111', 'd')
                .replace('\u0110', 'd');
        slug = NON_SLUG_CHARS_PATTERN.matcher(slug).replaceAll("").trim();
        slug = WHITESPACE_PATTERN.matcher(slug).replaceAll("-");
        slug = MULTI_DASH_PATTERN.matcher(slug).replaceAll("-");
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
        return VALID_SLUG_PATTERN.matcher(slug).matches();
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
}
