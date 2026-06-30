package com.aionn.sharedkernel.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class SlugUtilsPropertyTest {

    private static final Character[] VIETNAMESE_CHARS = {
            'à', 'á', 'ả', 'ã', 'ạ', 'ă', 'ắ', 'ằ', 'â', 'ấ', 'ầ',
            'é', 'è', 'ẻ', 'ẽ', 'ẹ', 'ê', 'ế', 'ề',
            'í', 'ì', 'ỉ', 'ĩ', 'ị',
            'ó', 'ò', 'ỏ', 'õ', 'ọ', 'ô', 'ố', 'ồ', 'ơ', 'ớ', 'ờ',
            'ú', 'ù', 'ủ', 'ũ', 'ụ', 'ư', 'ứ', 'ừ',
            'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ',
            'đ', 'Đ', 'À', 'Á', 'Ê', 'Ô', 'Ơ', 'Ư'
    };

    private static final Character[] SPECIAL_AND_WHITESPACE_CHARS = {
            ' ', '\t', '\n', '\r', '-', '!', '@', '#', '$', '%', '^', '&', '*',
            '(', ')', '_', '+', '=', '{', '}', '[', ']', '|', '\\', ':', ';',
            '"', '\'', '<', '>', ',', '.', '?', '/', '~', '`'
    };

    @Provide
    Arbitrary<String> mixedStrings() {
        Arbitrary<Character> asciiLetters = Arbitraries.oneOf(
                Arbitraries.chars().range('a', 'z'),
                Arbitraries.chars().range('A', 'Z'));
        Arbitrary<Character> digits = Arbitraries.chars().range('0', '9');
        Arbitrary<Character> vietnamese = Arbitraries.of(VIETNAMESE_CHARS);
        Arbitrary<Character> noise = Arbitraries.of(SPECIAL_AND_WHITESPACE_CHARS);

        Arbitrary<Character> anyChar = Arbitraries.oneOf(asciiLetters, digits, vietnamese, noise);
        return anyChar.list().ofMinSize(1).ofMaxSize(40).map(SlugUtilsPropertyTest::join);
    }

    @Provide
    Arbitrary<String> specialAndWhitespaceStrings() {
        return Arbitraries.of(SPECIAL_AND_WHITESPACE_CHARS)
                .list().ofMinSize(1).ofMaxSize(40)
                .map(SlugUtilsPropertyTest::join);
    }

    private static String join(List<Character> chars) {
        return chars.stream().map(String::valueOf).collect(Collectors.joining());
    }

    @Property(tries = 100)
    void property19_nonEmptySlugIsValid(@ForAll("mixedStrings") String input) {
        String slug = SlugUtils.slugify(input);
        Assume.that(!slug.isEmpty());

        assertTrue(SlugUtils.isValidSlug(slug),
                () -> "slugify(\"" + input + "\") = \"" + slug + "\" was expected to be a valid slug");
    }

    @Property(tries = 100)
    void property20_specialOnlyInputYieldsEmptySlug(@ForAll("specialAndWhitespaceStrings") String input) {
        assertEquals("", SlugUtils.slugify(input),
                () -> "slugify(\"" + input + "\") was expected to be empty");
        assertFalse(SlugUtils.isValidSlug(input),
                () -> "isValidSlug(\"" + input + "\") was expected to be false");
    }

    @Example
    void property20_nullInputYieldsEmptySlug() {
        assertEquals("", SlugUtils.slugify(null));
        assertFalse(SlugUtils.isValidSlug(null));
    }

    @Example
    void property20_emptyInputYieldsEmptySlug() {
        assertEquals("", SlugUtils.slugify(""));
        assertFalse(SlugUtils.isValidSlug(""));
    }
}
