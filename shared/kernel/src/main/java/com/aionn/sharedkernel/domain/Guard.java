package com.aionn.sharedkernel.domain;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class Guard {

    private Guard() {
    }

    public static <T> T notNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    public static <T> T notNull(T value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null) {
            throw exceptionSupplier.get();
        }
        return value;
    }

    public static String notBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public static String notBlank(String value, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (value == null || value.isBlank()) {
            throw exceptionSupplier.get();
        }
        return value.trim();
    }

    public static String maxLength(String value, int max, String fieldName) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(
                    fieldName + " must be at most " + max + " characters, got " + value.length());
        }
        return value;
    }

    public static String minLength(String value, int min, String fieldName) {
        if (value == null || value.length() < min) {
            throw new IllegalArgumentException(
                    fieldName + " must be at least " + min + " characters");
        }
        return value;
    }

    public static String matches(String value, Pattern pattern, String fieldName) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " does not match required pattern");
        }
        return value;
    }

    public static int positive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive, got " + value);
        }
        return value;
    }

    public static long positive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive, got " + value);
        }
        return value;
    }

    public static int nonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative, got " + value);
        }
        return value;
    }

    public static long nonNegative(long value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative, got " + value);
        }
        return value;
    }

    public static int between(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    fieldName + " must be between " + min + " and " + max + ", got " + value);
        }
        return value;
    }

    public static <C extends Collection<?>> C notEmpty(C collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return collection;
    }

    public static <M extends Map<?, ?>> M notEmpty(M map, String fieldName) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        return map;
    }

    public static <C extends Collection<?>> C maxSize(C collection, int max, String fieldName) {
        if (collection != null && collection.size() > max) {
            throw new IllegalArgumentException(
                    fieldName + " must contain at most " + max + " elements, got " + collection.size());
        }
        return collection;
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void require(boolean condition, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }

    public static void state(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    public static void state(boolean condition, Supplier<? extends RuntimeException> exceptionSupplier) {
        if (!condition) {
            throw exceptionSupplier.get();
        }
    }
}
