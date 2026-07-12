package com.aionn.sharedkernel.adapter.web.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

class PageMetadataPropertyTest {

        @Example
        void fromPaginationAndEmptyFactories() {
                PageMetadata fromPagination = PageMetadata.from(OffsetPagination.of(2, 20), 100);
                assertEquals(2, fromPagination.page());
                assertEquals(20, fromPagination.size());
                assertEquals(5, fromPagination.totalPages());

                PageMetadata empty = PageMetadata.empty(0, 20);
                assertEquals(0L, empty.totalElements());
                assertEquals(0, empty.totalPages());
        }

        @Property(tries = 100)
        void property16_metadataConsistentForValidInputs(
                        @ForAll @IntRange(min = 0, max = 100_000) int page,
                        @ForAll @IntRange(min = 1, max = 1_000) int size,
                        @ForAll @LongRange(min = 0, max = 1_000_000) long totalElements) {
                PageMetadata metadata = PageMetadata.of(page, size, totalElements);

                int expectedTotalPages = (int) Math.ceil((double) totalElements / size);

                assertEquals(expectedTotalPages, metadata.totalPages(),
                                () -> "totalPages must equal ceil(totalElements / size)");
                assertEquals(page == 0, metadata.isFirst(),
                                () -> "isFirst() must be (page == 0)");
                assertEquals(page >= metadata.totalPages() - 1, metadata.isLast(),
                                () -> "isLast() must be (page >= totalPages - 1)");
                assertEquals(page < metadata.totalPages() - 1, metadata.hasNext(),
                                () -> "hasNext() must be (page < totalPages - 1)");
                assertEquals(page > 0, metadata.hasPrevious(),
                                () -> "hasPrevious() must be (page > 0)");
        }

        @Property(tries = 100)
        void property16_nonPositiveSizeYieldsZeroTotalPages(
                        @ForAll @IntRange(min = 0, max = 100_000) int page,
                        @ForAll @IntRange(min = -100, max = 0) int size,
                        @ForAll @LongRange(min = 0, max = 1_000_000) long totalElements) {
                PageMetadata metadata = PageMetadata.of(page, size, totalElements);

                assertEquals(0, metadata.totalPages(),
                                () -> "totalPages must be 0 when size <= 0");
        }
}
