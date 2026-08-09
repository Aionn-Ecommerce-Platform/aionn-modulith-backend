package com.aionn.identity;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class TestClocks {

    public static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    public static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

    private TestClocks() {
    }
}
