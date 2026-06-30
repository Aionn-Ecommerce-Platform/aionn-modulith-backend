package com.aionn.sharedkernel.domain.vo;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record PhoneNumber(String value) {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");

    private static final Map<String, String> COUNTRY_CALLING_CODES = Map.ofEntries(
            Map.entry("VN", "+84"),
            Map.entry("US", "+1"),
            Map.entry("CA", "+1"),
            Map.entry("GB", "+44"),
            Map.entry("AU", "+61"),
            Map.entry("SG", "+65"),
            Map.entry("MY", "+60"),
            Map.entry("TH", "+66"),
            Map.entry("ID", "+62"),
            Map.entry("PH", "+63"),
            Map.entry("JP", "+81"),
            Map.entry("KR", "+82"),
            Map.entry("CN", "+86"),
            Map.entry("HK", "+852"),
            Map.entry("TW", "+886"),
            Map.entry("IN", "+91"));

    public PhoneNumber {
        Objects.requireNonNull(value, "PhoneNumber must not be null");
        value = normalize(value);

        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + value);
        }
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    public String toE164(String countryInput) {
        if (value.startsWith("+")) {
            return value;
        }
        String callingCode = resolveCallingCode(countryInput);
        if (callingCode == null) {
            throw new IllegalArgumentException(
                    "Unsupported country for phone normalization: " + countryInput);
        }
        String digits = value.startsWith("0") ? value.substring(1) : value;
        return callingCode + digits;
    }

    public boolean isE164() {
        if (!value.startsWith("+")) {
            return false;
        }
        int digitCount = value.length() - 1;
        return digitCount >= 8 && digitCount <= 15;
    }

    private static String resolveCallingCode(String countryInput) {
        if (countryInput == null || countryInput.isBlank()) {
            return null;
        }
        String trimmed = countryInput.trim();
        if (trimmed.startsWith("+")) {
            return trimmed;
        }
        String upper = trimmed.toUpperCase();
        String mapped = COUNTRY_CALLING_CODES.get(upper);
        if (mapped != null) {
            return mapped;
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return "+" + trimmed;
        }
        return null;
    }

    private static String normalize(String raw) {
        return raw.trim().replaceAll("[\\s\\-]", "");
    }
}
