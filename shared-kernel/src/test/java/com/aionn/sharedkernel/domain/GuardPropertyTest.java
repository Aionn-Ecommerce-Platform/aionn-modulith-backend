package com.aionn.sharedkernel.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.Tuple.Tuple2;
import net.jqwik.api.constraints.Positive;

/**
 * Property-based tests for {@link Guard}.
 *
 * <p>
 * Feature: shared-kernel-hardening. Implements Guard Properties 17-18 from the
 * design document, plus the {@code Guard.state(false, ...)} state-violation
 * case
 * (Requirement 5.3). Each property is exercised with at least 100 generated
 * inputs ({@code @Property(tries = 100)}).
 *
 * <p>
 * Validates: Requirements 5.1, 5.2, 5.3, 5.7
 */
class GuardPropertyTest {

    private static final Pattern LOWERCASE = Pattern.compile("[a-z]+");

    // =====================================================================
    // Generators
    // =====================================================================

    /** Non-blank strings, optionally padded with whitespace so trimming matters. */
    @Provide
    Arbitrary<String> nonBlankStrings() {
        Arbitrary<String> core = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20);
        Arbitrary<String> pad = Arbitraries.strings().withChars(' ', '\t').ofMaxLength(3);
        return Combinators.combine(pad, core, pad).as((p1, c, p2) -> p1 + c + p2);
    }

    /** Blank strings (empty or whitespace-only). */
    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.strings().withChars(' ', '\t', '\n', '\r').ofMinLength(0).ofMaxLength(6);
    }

    /** Lowercase strings that match {@link #LOWERCASE}. */
    @Provide
    Arbitrary<String> lowercaseStrings() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(20);
    }

    /** Digit-only strings that never match {@link #LOWERCASE}. */
    @Provide
    Arbitrary<String> digitStrings() {
        return Arbitraries.strings().withCharRange('0', '9').ofMinLength(1).ofMaxLength(20);
    }

    /** Integers &lt;= 0. */
    @Provide
    Arbitrary<Integer> nonPositiveInts() {
        return Arbitraries.integers().lessOrEqual(0);
    }

    /** Longs &lt;= 0. */
    @Provide
    Arbitrary<Long> nonPositiveLongs() {
        return Arbitraries.longs().lessOrEqual(0L);
    }

    /** Integers &gt;= 0. */
    @Provide
    Arbitrary<Integer> nonNegativeInts() {
        return Arbitraries.integers().greaterOrEqual(0);
    }

    /** Longs &gt;= 0. */
    @Provide
    Arbitrary<Long> nonNegativeLongs() {
        return Arbitraries.longs().greaterOrEqual(0L);
    }

    /** Integers &lt; 0. */
    @Provide
    Arbitrary<Integer> negativeInts() {
        return Arbitraries.integers().lessOrEqual(-1);
    }

    /** Longs &lt; 0. */
    @Provide
    Arbitrary<Long> negativeLongs() {
        return Arbitraries.longs().lessOrEqual(-1L);
    }

    /** {value, min, max} with min &lt;= value &lt;= max. */
    @Provide
    Arbitrary<int[]> betweenValid() {
        Arbitrary<Integer> n = Arbitraries.integers().between(-10_000, 10_000);
        return Combinators.combine(n, n, n).as((a, b, c) -> {
            int[] arr = { a, b, c };
            Arrays.sort(arr);
            return new int[] { arr[1], arr[0], arr[2] };
        });
    }

    /** {value, min, max} with value strictly outside [min, max]. */
    @Provide
    Arbitrary<int[]> betweenInvalid() {
        return Combinators.combine(
                Arbitraries.integers().between(-1_000, 1_000),
                Arbitraries.integers().between(0, 1_000),
                Arbitraries.integers().between(1, 1_000),
                Arbitraries.of(true, false))
                .as((min, span, delta, below) -> {
                    int max = min + span;
                    int value = below ? (min - delta) : (max + delta);
                    return new int[] { value, min, max };
                });
    }

    /** Non-empty list of integers. */
    @Provide
    Arbitrary<List<Integer>> nonEmptyLists() {
        return Arbitraries.integers().between(0, 100).list().ofMinSize(1).ofMaxSize(20);
    }

    /** Non-empty map. */
    @Provide
    Arbitrary<Map<String, Integer>> nonEmptyMaps() {
        return Arbitraries.maps(
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(5),
                Arbitraries.integers()).ofMinSize(1).ofMaxSize(20);
    }

    /** (list, max) with list.size() &lt;= max. */
    @Provide
    Arbitrary<Tuple2<List<Integer>, Integer>> listWithinMax() {
        return Arbitraries.integers().between(0, 100).list().ofMaxSize(20)
                .flatMap(list -> Arbitraries.integers().between(list.size(), list.size() + 10)
                        .map(max -> Tuple.of(list, max)));
    }

    /** (list, max) with list non-empty and list.size() &gt; max. */
    @Provide
    Arbitrary<Tuple2<List<Integer>, Integer>> listOverMax() {
        return Arbitraries.integers().between(0, 100).list().ofMinSize(1).ofMaxSize(20)
                .flatMap(list -> Arbitraries.integers().between(0, list.size() - 1)
                        .map(max -> Tuple.of(list, max)));
    }

    /** (string, max) with string.length() &lt;= max. */
    @Provide
    Arbitrary<Tuple2<String, Integer>> stringWithinMax() {
        return Arbitraries.strings().ofMaxLength(20)
                .flatMap(s -> Arbitraries.integers().between(s.length(), s.length() + 10)
                        .map(max -> Tuple.of(s, max)));
    }

    /** (string, max) with string non-empty and string.length() &gt; max. */
    @Provide
    Arbitrary<Tuple2<String, Integer>> stringOverMax() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(20)
                .flatMap(s -> Arbitraries.integers().between(0, s.length() - 1)
                        .map(max -> Tuple.of(s, max)));
    }

    /** (string, min) with string.length() &gt;= min. */
    @Provide
    Arbitrary<Tuple2<String, Integer>> stringAtLeastMin() {
        return Arbitraries.strings().ofMaxLength(20)
                .flatMap(s -> Arbitraries.integers().between(0, s.length())
                        .map(min -> Tuple.of(s, min)));
    }

    /** (string, min) with string.length() &lt; min. */
    @Provide
    Arbitrary<Tuple2<String, Integer>> stringBelowMin() {
        return Arbitraries.strings().ofMaxLength(20)
                .flatMap(s -> Arbitraries.integers().between(s.length() + 1, s.length() + 10)
                        .map(min -> Tuple.of(s, min)));
    }

    // =====================================================================
    // Property 17 — valid input is returned unchanged (no exception)
    // Feature: shared-kernel-hardening, Property 17
    // =====================================================================

    @Property(tries = 100)
    void property17_notNull_returnsValue(@ForAll String value) {
        assertSame(value, Guard.notNull(value, "field"));
    }

    @Property(tries = 100)
    void property17_notBlank_returnsTrimmedValue(@ForAll("nonBlankStrings") String value) {
        assertEquals(value.trim(), Guard.notBlank(value, "field"));
    }

    @Property(tries = 100)
    void property17_maxLength_returnsValue(@ForAll("stringWithinMax") Tuple2<String, Integer> input) {
        String value = input.get1();
        int max = input.get2();
        assertSame(value, Guard.maxLength(value, max, "field"));
    }

    @Property(tries = 100)
    void property17_minLength_returnsValue(@ForAll("stringAtLeastMin") Tuple2<String, Integer> input) {
        String value = input.get1();
        int min = input.get2();
        assertSame(value, Guard.minLength(value, min, "field"));
    }

    @Property(tries = 100)
    void property17_matches_returnsValue(@ForAll("lowercaseStrings") String value) {
        assertSame(value, Guard.matches(value, LOWERCASE, "field"));
    }

    @Property(tries = 100)
    void property17_positiveInt_returnsValue(@ForAll @Positive int value) {
        assertEquals(value, Guard.positive(value, "field"));
    }

    @Property(tries = 100)
    void property17_positiveLong_returnsValue(@ForAll @Positive long value) {
        assertEquals(value, Guard.positive(value, "field"));
    }

    @Property(tries = 100)
    void property17_nonNegativeInt_returnsValue(@ForAll("nonNegativeInts") int value) {
        assertEquals(value, Guard.nonNegative(value, "field"));
    }

    @Property(tries = 100)
    void property17_nonNegativeLong_returnsValue(@ForAll("nonNegativeLongs") long value) {
        assertEquals(value, Guard.nonNegative(value, "field"));
    }

    @Property(tries = 100)
    void property17_between_returnsValue(@ForAll("betweenValid") int[] t) {
        int value = t[0];
        int min = t[1];
        int max = t[2];
        assertEquals(value, Guard.between(value, min, max, "field"));
    }

    @Property(tries = 100)
    void property17_notEmptyCollection_returnsValue(@ForAll("nonEmptyLists") List<Integer> collection) {
        assertSame(collection, Guard.notEmpty(collection, "field"));
    }

    @Property(tries = 100)
    void property17_notEmptyMap_returnsValue(@ForAll("nonEmptyMaps") Map<String, Integer> map) {
        assertSame(map, Guard.notEmpty(map, "field"));
    }

    @Property(tries = 100)
    void property17_maxSize_returnsValue(@ForAll("listWithinMax") Tuple2<List<Integer>, Integer> input) {
        List<Integer> collection = input.get1();
        int max = input.get2();
        assertSame(collection, Guard.maxSize(collection, max, "field"));
    }

    @Property(tries = 100)
    void property17_requireTrue_doesNotThrow(@ForAll String message) {
        assertDoesNotThrow(() -> Guard.require(true, message));
    }

    @Property(tries = 100)
    void property17_stateTrue_doesNotThrow(@ForAll String message) {
        assertDoesNotThrow(() -> Guard.state(true, message));
    }

    // =====================================================================
    // Property 18 — invalid value-guard input throws IllegalArgumentException
    // Feature: shared-kernel-hardening, Property 18
    // =====================================================================

    @Example
    void property18_notNull_null_throwsIae() {
        assertThrows(IllegalArgumentException.class, () -> Guard.notNull((Object) null, "field"));
    }

    @Property(tries = 100)
    void property18_notBlank_blank_throwsIae(@ForAll("blankStrings") String value) {
        assertThrows(IllegalArgumentException.class, () -> Guard.notBlank(value, "field"));
    }

    @Property(tries = 100)
    void property18_maxLength_over_throwsIae(@ForAll("stringOverMax") Tuple2<String, Integer> input) {
        String value = input.get1();
        int max = input.get2();
        assertThrows(IllegalArgumentException.class, () -> Guard.maxLength(value, max, "field"));
    }

    @Property(tries = 100)
    void property18_minLength_short_throwsIae(@ForAll("stringBelowMin") Tuple2<String, Integer> input) {
        String value = input.get1();
        int min = input.get2();
        assertThrows(IllegalArgumentException.class, () -> Guard.minLength(value, min, "field"));
    }

    @Property(tries = 100)
    void property18_matches_nonMatching_throwsIae(@ForAll("digitStrings") String value) {
        assertThrows(IllegalArgumentException.class, () -> Guard.matches(value, LOWERCASE, "field"));
    }

    @Property(tries = 100)
    void property18_positiveInt_nonPositive_throwsIae(@ForAll("nonPositiveInts") int value) {
        assertThrows(IllegalArgumentException.class, () -> Guard.positive(value, "field"));
    }

    @Property(tries = 100)
    void property18_positiveLong_nonPositive_throwsIae(@ForAll("nonPositiveLongs") long value) {
        assertThrows(IllegalArgumentException.class, () -> Guard.positive(value, "field"));
    }

    @Property(tries = 100)
    void property18_nonNegativeInt_negative_throwsIae(@ForAll("negativeInts") int value) {
        assertThrows(IllegalArgumentException.class, () -> Guard.nonNegative(value, "field"));
    }

    @Property(tries = 100)
    void property18_nonNegativeLong_negative_throwsIae(@ForAll("negativeLongs") long value) {
        assertThrows(IllegalArgumentException.class, () -> Guard.nonNegative(value, "field"));
    }

    @Property(tries = 100)
    void property18_between_outside_throwsIae(@ForAll("betweenInvalid") int[] t) {
        int value = t[0];
        int min = t[1];
        int max = t[2];
        assertThrows(IllegalArgumentException.class, () -> Guard.between(value, min, max, "field"));
    }

    @Example
    void property18_notEmptyCollection_empty_throwsIae() {
        assertThrows(IllegalArgumentException.class, () -> Guard.notEmpty(Collections.emptyList(), "field"));
        assertThrows(IllegalArgumentException.class, () -> Guard.notEmpty((List<Integer>) null, "field"));
    }

    @Example
    void property18_notEmptyMap_empty_throwsIae() {
        assertThrows(IllegalArgumentException.class, () -> Guard.notEmpty(Collections.emptyMap(), "field"));
        assertThrows(IllegalArgumentException.class, () -> Guard.notEmpty((Map<String, Integer>) null, "field"));
    }

    @Property(tries = 100)
    void property18_maxSize_over_throwsIae(@ForAll("listOverMax") Tuple2<List<Integer>, Integer> input) {
        List<Integer> collection = input.get1();
        int max = input.get2();
        assertThrows(IllegalArgumentException.class, () -> Guard.maxSize(collection, max, "field"));
    }

    @Property(tries = 100)
    void property18_requireFalse_throwsIae(@ForAll String message) {
        assertThrows(IllegalArgumentException.class, () -> Guard.require(false, message));
    }

    // =====================================================================
    // Requirement 5.3 — state(false, ...) throws IllegalStateException
    // Feature: shared-kernel-hardening, Property 18 (state variant)
    // =====================================================================

    @Property(tries = 100)
    void stateFalse_throwsIllegalStateException(@ForAll String message) {
        assertThrows(IllegalStateException.class, () -> Guard.state(false, message));
    }
}
