package com.aionn.sharedkernel.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class SortDirectionPropertyTest {

    @Property(tries = 100)
    void property13_descVariantsMapToDesc(@ForAll("descCaseVariants") String input) {
        assertEquals(SortDirection.DESC, SortDirection.from(input),
                () -> "from(\"" + input + "\") should be DESC");
    }

    @Property(tries = 100)
    void property13_ascNullAndBlankMapToAsc(@ForAll("ascNullBlankInputs") String input) {
        assertEquals(SortDirection.ASC, SortDirection.from(input),
                () -> "from(" + (input == null ? "null" : "\"" + input + "\"") + ") should be ASC");
    }

    @Property(tries = 100)
    void property13_nonDescStringsMapToAsc(@ForAll("nonDescStrings") String input) {
        assertEquals(SortDirection.ASC, SortDirection.from(input),
                () -> "from(\"" + input + "\") should be ASC");
    }

    @Provide
    Arbitrary<String> descCaseVariants() {
        return randomCaseVariants("desc");
    }

    @Provide
    Arbitrary<String> ascNullBlankInputs() {
        Arbitrary<String> ascVariants = randomCaseVariants("asc");
        Arbitrary<String> blanks = Arbitraries.of("", " ", "  ", "\t", "\n", "   ", " \t \n ");
        return Arbitraries.oneOf(ascVariants, blanks).injectNull(0.2);
    }

    @Provide
    Arbitrary<String> nonDescStrings() {
        return Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(20)
                .filter(s -> !s.equalsIgnoreCase("desc"));
    }

    private Arbitrary<String> randomCaseVariants(String base) {
        Arbitrary<List<Boolean>> upperFlags = Arbitraries.of(true, false).list().ofSize(base.length());
        return upperFlags.map(flags -> {
            StringBuilder sb = new StringBuilder(base.length());
            for (int i = 0; i < base.length(); i++) {
                char c = base.charAt(i);
                sb.append(flags.get(i) ? Character.toUpperCase(c) : Character.toLowerCase(c));
            }
            return sb.toString();
        });
    }
}
