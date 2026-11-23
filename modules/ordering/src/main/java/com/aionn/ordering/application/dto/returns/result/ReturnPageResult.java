package com.aionn.ordering.application.dto.returns.result;

import java.util.List;

public record ReturnPageResult(List<ReturnResult> content, long totalElements) {
}
