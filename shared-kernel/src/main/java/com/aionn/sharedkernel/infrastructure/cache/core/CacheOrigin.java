package com.aionn.sharedkernel.infrastructure.cache.core;

import java.util.UUID;

public final class CacheOrigin {

    private final String value;

    public CacheOrigin(String configuredOrigin) {
        this.value = (configuredOrigin == null || configuredOrigin.isBlank())
                ? UUID.randomUUID().toString()
                : configuredOrigin;
    }

    public String value() {
        return value;
    }
}
