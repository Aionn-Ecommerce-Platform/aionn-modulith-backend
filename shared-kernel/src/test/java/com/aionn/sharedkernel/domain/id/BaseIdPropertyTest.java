package com.aionn.sharedkernel.domain.id;

import com.aionn.sharedkernel.util.IdGenerator;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaseIdPropertyTest {

    @Provide
    Arbitrary<String> validUlids() {
        return Arbitraries.longs().between(0L, 281_474_976_710_655L)
                .map(IdGenerator::ulid);
    }

    @Provide
    Arbitrary<String> invalidUlidStrings() {
        Arbitrary<String> wrongLength = Arbitraries.strings()
                .withChars("0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray())
                .ofMinLength(0).ofMaxLength(40)
                .filter(s -> s.length() != 26);

        Arbitrary<String> withExcludedChar = Combinators.combine(
                Arbitraries.strings()
                        .withChars("0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray())
                        .ofLength(25),
                Arbitraries.of('I', 'L', 'O', 'U'),
                Arbitraries.integers().between(0, 25))
                .as((base, excluded, pos) -> {
                    StringBuilder sb = new StringBuilder(base);
                    sb.insert((int) pos, (char) excluded);
                    return sb.toString();
                });

        Arbitrary<String> empty = Arbitraries.just("");

        return Arbitraries.oneOf(wrongLength, withExcludedChar, empty)
                .filter(s -> !IdGenerator.isValid(s));
    }

    @Property(tries = 1000)
    void baseIdPreservesValueAndEquality(@ForAll("validUlids") String ulid) {
        TestId id = new TestId(ulid);
        assertNotNull(id);
        assertEquals(ulid, id.getValue());
        assertEquals(ulid, id.toString());

        TestId other = new TestId(ulid);
        assertEquals(id, other);
        assertEquals(other, id);
        assertEquals(id.hashCode(), other.hashCode());
    }

    @Property(tries = 1000)
    void baseIdRejectsInvalidStrings(@ForAll("invalidUlidStrings") String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new TestId(invalid));
    }

    @Property(tries = 1000)
    void baseIdRejectsNull(@ForAll("validUlids") String ignored) {
        assertThrows(NullPointerException.class, () -> new TestId(null));
    }
}
