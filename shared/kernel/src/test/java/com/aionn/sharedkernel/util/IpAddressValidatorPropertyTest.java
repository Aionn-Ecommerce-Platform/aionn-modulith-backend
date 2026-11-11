package com.aionn.sharedkernel.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class IpAddressValidatorPropertyTest {

    @Provide
    Arbitrary<String> validIpv4() {
        Arbitrary<Integer> octet = Arbitraries.integers().between(0, 255);
        return Combinators.combine(octet, octet, octet, octet)
                .as((a, b, c, d) -> a + "." + b + "." + c + "." + d);
    }

    @Provide
    Arbitrary<String> validIpv6() {
        return Arbitraries.integers().between(0, 0xFFFF)
                .list().ofSize(8)
                .map(groups -> groups.stream()
                        .map(Integer::toHexString)
                        .collect(Collectors.joining(":")));
    }

    @Provide
    Arbitrary<String> nonIpStrings() {
        return Arbitraries.strings().withCharRange('g', 'z').ofMinLength(1).ofMaxLength(20);
    }

    @Property(tries = 100)
    void property21_validIpv4IsValid(@ForAll("validIpv4") String ip) {
        assertTrue(IpAddressValidator.isValid(ip),
                () -> "Expected IPv4 \"" + ip + "\" to be valid");
    }

    @Property(tries = 100)
    void property21_validIpv6IsValid(@ForAll("validIpv6") String ip) {
        assertTrue(IpAddressValidator.isValid(ip),
                () -> "Expected IPv6 \"" + ip + "\" to be valid");
    }

    @Property(tries = 100)
    void property21_nonIpStringIsInvalid(@ForAll("nonIpStrings") String value) {
        assertFalse(IpAddressValidator.isValid(value),
                () -> "Expected \"" + value + "\" to be invalid");
    }

    @Example
    void property21_nullIsInvalid() {
        assertFalse(IpAddressValidator.isValid(null));
    }

    @Example
    void property21_emptyAndBlankAreInvalid() {
        assertFalse(IpAddressValidator.isValid(""));
        assertFalse(IpAddressValidator.isValid("   "));
        assertFalse(IpAddressValidator.isValid("\t"));
    }

    @Example
    void property21_literalPassingCharsetButUnparsableIsInvalid() {
        // Only contains allowed characters (digits + colons) so it passes the
        // character allow-list, but is not a valid IPv6 literal; InetAddress
        // rejects it as a literal without any DNS lookup.
        assertFalse(IpAddressValidator.isValid("1:2:3"));
    }

    @Example
    void property21_validateAcceptsValidAndRejectsInvalid() {
        IpAddressValidator.validate("203.0.113.5");
        assertThrows(IllegalArgumentException.class, () -> IpAddressValidator.validate("not-an-ip"));
    }
}
