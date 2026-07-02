package com.aionn.identity.application.dto.common;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements) {

    public PageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }
}
