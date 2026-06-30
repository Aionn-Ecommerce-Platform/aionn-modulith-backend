package com.aionn.sharedkernel.util;

import java.util.regex.Pattern;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdGeneratorPropertyTest {

    private static final long MAX_TIMESTAMP = 281_474_976_710_655L;

    private static final Pattern ULID_PATTERN = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$");

    @Provide
    Arbitrary<Long> timestamps() {
        return Arbitraries.longs().between(0L, MAX_TIMESTAMP);
    }

    @Property(tries = 1000)
    void generatedUlidIsAlwaysValid(@ForAll("timestamps") long timestampMs) {
        String fromTimestamp = IdGenerator.ulid(timestampMs);
        assertEquals(26, fromTimestamp.length());
        assertTrue(ULID_PATTERN.matcher(fromTimestamp).matches(),
                () -> "ULID does not match pattern: " + fromTimestamp);
        assertTrue(IdGenerator.isValid(fromTimestamp),
                () -> "isValid returned false for: " + fromTimestamp);

        String fromNow = IdGenerator.ulid();
        assertEquals(26, fromNow.length());
        assertTrue(ULID_PATTERN.matcher(fromNow).matches(),
                () -> "ULID does not match pattern: " + fromNow);
        assertTrue(IdGenerator.isValid(fromNow),
                () -> "isValid returned false for: " + fromNow);
    }

    @Property(tries = 1000)
    void timestampRoundTrips(@ForAll("timestamps") long timestampMs) {
        String ulid = IdGenerator.ulid(timestampMs);
        assertEquals(timestampMs, IdGenerator.extractTimestamp(ulid));
    }
}
