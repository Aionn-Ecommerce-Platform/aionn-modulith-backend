package com.aionn.sharedkernel.adapter.web.response;

import com.aionn.sharedkernel.domain.vo.OffsetPagination;

public record PageMetadata(
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static PageMetadata of(int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageMetadata(page, size, totalElements, totalPages);
    }

    public static PageMetadata from(OffsetPagination pagination, long totalElements) {
        return of(pagination.page(), pagination.size(), totalElements);
    }

    public static PageMetadata empty(int page, int size) {
        return new PageMetadata(page, size, 0L, 0);
    }

    public boolean isFirst() {
        return page == 0;
    }

    public boolean isLast() {
        return page >= totalPages - 1;
    }

    public boolean hasNext() {
        return page < totalPages - 1;
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}
