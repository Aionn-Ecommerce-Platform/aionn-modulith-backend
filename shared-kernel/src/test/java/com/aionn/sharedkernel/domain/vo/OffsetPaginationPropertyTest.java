package com.aionn.sharedkernel.domain.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

class OffsetPaginationPropertyTest {

        @Property(tries = 100)
        void property11_safeClampsOversizeToMaxSize(
                        @ForAll int page,
                        @ForAll @IntRange(min = OffsetPagination.MAX_SIZE + 1) int oversize) {
                OffsetPagination result = OffsetPagination.safe(page, oversize);
                assertEquals(OffsetPagination.MAX_SIZE, result.size());
        }

        @Property(tries = 100)
        void property11_offsetEqualsPageTimesSizeAndNonNegative(
                        @ForAll @IntRange(min = 0, max = 100_000) int page,
                        @ForAll @IntRange(min = 1, max = OffsetPagination.MAX_SIZE) int size) {
                OffsetPagination pagination = OffsetPagination.of(page, size);

                assertEquals(page * size, pagination.offset());
                assertTrue(pagination.offset() >= 0,
                                () -> "offset() must be non-negative but was " + pagination.offset());
        }

        @Property(tries = 100)
        void property12_constructorRejectsOutOfDomain(@ForAll("invalidPageSize") int[] pageSize) {
                int page = pageSize[0];
                int size = pageSize[1];

                assertThrows(IllegalArgumentException.class,
                                () -> new OffsetPagination(page, size, null, null));
                assertThrows(IllegalArgumentException.class,
                                () -> OffsetPagination.of(page, size));
        }

        @Provide
        Arbitrary<int[]> invalidPageSize() {
                Arbitrary<int[]> negativePage = Combinators.combine(
                                Arbitraries.integers().between(Integer.MIN_VALUE / 2, -1),
                                Arbitraries.integers().between(1, OffsetPagination.MAX_SIZE))
                                .as((p, s) -> new int[] { p, s });

                Arbitrary<int[]> sizeTooSmall = Combinators.combine(
                                Arbitraries.integers().between(0, 100_000),
                                Arbitraries.integers().between(Integer.MIN_VALUE / 2, 0))
                                .as((p, s) -> new int[] { p, s });

                Arbitrary<int[]> sizeTooLarge = Combinators.combine(
                                Arbitraries.integers().between(0, 100_000),
                                Arbitraries.integers().between(OffsetPagination.MAX_SIZE + 1, Integer.MAX_VALUE))
                                .as((p, s) -> new int[] { p, s });

                return Arbitraries.oneOf(negativePage, sizeTooSmall, sizeTooLarge);
        }
}
