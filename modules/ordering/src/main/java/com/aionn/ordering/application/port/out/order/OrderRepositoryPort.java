package com.aionn.ordering.application.port.out.order;

import com.aionn.ordering.domain.model.Order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(String orderId);

    List<Order> findByUser(String userId, int limit);

    List<Order> findByMerchant(String merchantId, int limit);

    List<Order> findByMerchantAndStatus(String merchantId, String status, int limit);

    List<Order> findByUserAndStatus(String userId, String status, int limit);

    List<Order> findByMerchantBetween(String merchantId, Instant from, Instant to, int limit);

    List<Order> findPendingOlderThan(Instant cutoff, int limit);
}
