package com.aionn.sharedkernel.domain.id;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

public abstract class BaseId implements Serializable {

    private static final Pattern ULID_PATTERN = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$");

    private final String value;

    protected BaseId(String value) {
        Objects.requireNonNull(value, "ID value must not be null");
        if (!ULID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("ID must be a valid ULID (26 Crockford Base32 chars), got: " + value);
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BaseId baseId = (BaseId) o;
        return Objects.equals(value, baseId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
