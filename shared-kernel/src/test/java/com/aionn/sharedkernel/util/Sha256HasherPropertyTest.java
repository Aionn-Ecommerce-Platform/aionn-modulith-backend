package com.aionn.sharedkernel.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class Sha256HasherPropertyTest {

    @Example
    void matchesKnownSha256Vector() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                Sha256Hasher.hexDigest("abc"));
    }

    @Property(tries = 100)
    void digestIsDeterministicAndLowercaseHexOfFixedLength(@ForAll("anyValue") String value) {
        String first = Sha256Hasher.hexDigest(value);
        String second = Sha256Hasher.hexDigest(value);

        assertEquals(first, second);
        assertEquals(64, first.length());
        assertTrue(first.matches("[0-9a-f]{64}"),
                () -> "Expected lowercase hex digest, but was: " + first);
    }

    @Provide
    Arbitrary<String> anyValue() {
        return Arbitraries.strings().ofMaxLength(64);
    }
}
