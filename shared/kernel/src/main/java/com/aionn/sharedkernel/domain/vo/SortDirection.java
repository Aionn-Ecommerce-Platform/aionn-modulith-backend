package com.aionn.sharedkernel.domain.vo;

public enum SortDirection {

    ASC,
    DESC;

    public static SortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        return value.equalsIgnoreCase("desc") ? DESC : ASC;
    }

    public boolean isDescending() {
        return this == DESC;
    }
}
