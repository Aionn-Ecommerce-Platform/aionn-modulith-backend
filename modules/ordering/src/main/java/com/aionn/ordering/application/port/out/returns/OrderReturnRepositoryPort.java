package com.aionn.ordering.application.port.out.returns;

import com.aionn.ordering.domain.model.OrderReturn;

import java.util.List;
import java.util.Optional;

public interface OrderReturnRepositoryPort {

    OrderReturn save(OrderReturn orderReturn);

    Optional<OrderReturn> findById(String returnId);

    List<OrderReturn> findByMerchant(String merchantId, int limit);

    List<OrderReturn> findByUser(String userId, int limit);
}
