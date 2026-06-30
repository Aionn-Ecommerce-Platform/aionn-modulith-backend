package com.aionn.sharedkernel.domain.vo;

public record OffsetPagination(
        int page,
        int size,
        String sortBy,
        SortDirection sortDir) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public OffsetPagination {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0, got " + page);
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be >= 1, got " + size);
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be <= " + MAX_SIZE + ", got " + size);
        }
        if (sortDir == null) {
            sortDir = SortDirection.ASC;
        }
    }

    public static OffsetPagination of(int page, int size) {
        return new OffsetPagination(page, size, null, SortDirection.ASC);
    }

    public static OffsetPagination of(int page, int size, String sortBy, String sortDir) {
        return new OffsetPagination(page, size, sortBy, SortDirection.from(sortDir));
    }

    public static OffsetPagination of(int page, int size, String sortBy, SortDirection sortDir) {
        return new OffsetPagination(page, size, sortBy, sortDir);
    }

    public static OffsetPagination defaultPage() {
        return of(DEFAULT_PAGE, DEFAULT_SIZE);
    }

    public static OffsetPagination safe(int page, int size) {
        int safePage = Math.max(page, DEFAULT_PAGE);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return of(safePage, safeSize);
    }

    public static OffsetPagination safe(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, DEFAULT_PAGE);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return of(safePage, safeSize, sortBy, sortDir);
    }

    public int offset() {
        return page * size;
    }

    public int getOffset() {
        return offset();
    }

    public boolean isFirstPage() {
        return page == 0;
    }

    public boolean isSortDesc() {
        return sortDir.isDescending();
    }

    public boolean hasSorting() {
        return sortBy != null && !sortBy.isBlank();
    }
}
