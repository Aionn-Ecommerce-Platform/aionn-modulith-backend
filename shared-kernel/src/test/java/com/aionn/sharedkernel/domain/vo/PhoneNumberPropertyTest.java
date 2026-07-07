package com.aionn.sharedkernel.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class PhoneNumberPropertyTest {

    @Example
    void nullValueIsRejected() {
        assertThrows(NullPointerException.class, () -> PhoneNumber.of(null));
    }

    @Example
    void normalizesWhitespaceAndDashes() {
        assertEquals("+84912345678", PhoneNumber.of(" +84 912-345-678 ").value());
    }

    @Example
    void localNumberIsNotE164() {
        assertFalse(PhoneNumber.of("0912345678").isE164());
    }

    @Example
    void toE164KeepsAlreadyPrefixedValue() {
        assertEquals("+84912345678", PhoneNumber.of("+84912345678").toE164("VN"));
    }

    @Example
    void toE164UsesKnownCountryCodeAndStripsLeadingZero() {
        assertEquals("+84912345678", PhoneNumber.of("0912345678").toE164("VN"));
    }

    @Example
    void toE164LowercaseCountryCodeIsResolved() {
        assertEquals("+84912345678", PhoneNumber.of("0912345678").toE164("vn"));
    }

    @Example
    void toE164AcceptsExplicitPlusCallingCode() {
        assertEquals("+84912345678", PhoneNumber.of("912345678").toE164("+84"));
    }

    @Example
    void toE164AcceptsNumericCallingCode() {
        assertEquals("+84912345678", PhoneNumber.of("912345678").toE164("84"));
    }

    @Example
    void toE164RejectsNullCountry() {
        PhoneNumber phone = PhoneNumber.of("0912345678");
        assertThrows(IllegalArgumentException.class, () -> phone.toE164(null));
    }

    @Example
    void toE164RejectsBlankCountry() {
        PhoneNumber phone = PhoneNumber.of("0912345678");
        assertThrows(IllegalArgumentException.class, () -> phone.toE164("   "));
    }

    @Example
    void toE164RejectsUnsupportedCountry() {
        PhoneNumber phone = PhoneNumber.of("0912345678");
        assertThrows(IllegalArgumentException.class, () -> phone.toE164("ZZ"));
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");

    @Property(tries = 100)
    void validE164IsRecognizedAndNormalized(@ForAll("validE164") String input) {
        PhoneNumber phone = PhoneNumber.of(input);

        assertTrue(phone.isE164(), () -> "Expected isE164() == true for " + input);

        assertOneLeadingPlusThenDigits(phone.toE164("VN"));
        assertOneLeadingPlusThenDigits(phone.toE164("+999"));
        assertOneLeadingPlusThenDigits(phone.toE164(null));
    }

    @Property(tries = 100)
    void invalidFormatIsRejected(@ForAll("invalidPhone") String input) {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumber.of(input));
    }

    private static void assertOneLeadingPlusThenDigits(String e164) {
        assertTrue(e164.matches("^\\+[0-9]+$"),
                () -> "Expected exactly one leading '+' followed only by digits, but was: " + e164);
    }

    private static String normalize(String raw) {
        return raw.trim().replaceAll("[\\s\\-]", "");
    }

    @Provide
    Arbitrary<String> validE164() {
        return Arbitraries.integers().between(8, 15)
                .flatMap(len -> Arbitraries.strings().withCharRange('1', '9').ofLength(1)
                        .flatMap(first -> Arbitraries.strings().withCharRange('0', '9').ofLength(len - 1)
                                .map(rest -> "+" + first + rest)));
    }

    @Provide
    Arbitrary<String> invalidPhone() {
        Arbitrary<String> tooShort = withOptionalPlus(
                Arbitraries.integers().between(1, 7)
                        .flatMap(n -> Arbitraries.strings().withCharRange('0', '9').ofLength(n)));

        Arbitrary<String> tooLong = withOptionalPlus(
                Arbitraries.integers().between(16, 25)
                        .flatMap(n -> Arbitraries.strings().withCharRange('0', '9').ofLength(n)));

        Arbitrary<String> withLetters = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(1).ofMaxLength(20)
                .filter(s -> s.chars().anyMatch(Character::isLetter));

        Arbitrary<String> empty = Arbitraries.just("");

        return Arbitraries.oneOf(tooShort, tooLong, withLetters, empty)
                .filter(s -> !PHONE_PATTERN.matcher(normalize(s)).matches());
    }

    private Arbitrary<String> withOptionalPlus(Arbitrary<String> base) {
        return base.flatMap(s -> Arbitraries.of("+", "").map(prefix -> prefix + s));
    }
}
