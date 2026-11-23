package com.aionn.ordering.application.dto.order.result;

import java.util.List;

public record OrderPageResult(List<OrderResult> content, long totalElements) {
}
