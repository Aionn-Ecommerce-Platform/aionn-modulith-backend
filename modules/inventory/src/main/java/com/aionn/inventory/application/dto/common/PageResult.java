package com.aionn.inventory.application.dto.common;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        int returned,
        long totalElements) {
}
