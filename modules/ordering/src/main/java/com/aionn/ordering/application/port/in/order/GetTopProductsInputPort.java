package com.aionn.ordering.application.port.in.order;

import com.aionn.ordering.application.dto.order.result.TopProductResult;
import java.time.LocalDate;
import java.util.List;

public interface GetTopProductsInputPort {
    List<TopProductResult> execute(String ownerId, LocalDate from, LocalDate to, int limit);
}