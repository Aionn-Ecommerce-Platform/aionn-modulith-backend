package com.aionn.ordering.infrastructure.integration;

import com.aionn.ordering.application.port.out.OrderPersistencePort;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.sharedkernel.integration.port.ordering.OrderSnapshotQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ordering-side adapter that translates Order aggregates into transport-neutral
 * snapshots for cross-service callers. Permalinks are intentionally NOT in the
 * snapshot â€” those are caller (UCP) concerns since they encode the storefront
 * base URL, which ordering does not own.
 */
@Component
@RequiredArgsConstructor
public class OrderingOrderSnapshotAdapter implements OrderSnapshotQueryPort {

    private final OrderPersistencePort orderRepository;

    @Override
    public Optional<OrderSnapshot> findOrderById(String orderId) {
        return orderRepository.findById(orderId).map(OrderingOrderSnapshotAdapter::toSnapshot);
    }

    private static OrderSnapshot toSnapshot(Order order) {
        List<OrderSnapshot.Line> lines = new ArrayList<>(order.items().size());
        long subtotalMinor = 0;
        for (OrderItem item : order.items()) {
            long unit = item.unitPrice().amount().longValue();
            long lineTotal = unit * item.qty();
            subtotalMinor += lineTotal;
            lines.add(new OrderSnapshot.Line(item.skuId(), item.qty(), unit, lineTotal));
        }
        long shippingMinor = order.getShippingFee() == null ? 0L : order.getShippingFee().amount().longValue();
        long totalMinor = order.getTotalAmount() == null
                ? subtotalMinor + shippingMinor
                : order.getTotalAmount().amount().longValue();
        return new OrderSnapshot(
                order.getOrderId(),
                order.getUserId(),
                order.getMerchantId(),
                order.getCurrency(),
                order.getStatus().name(),
                subtotalMinor,
                shippingMinor,
                totalMinor,
                lines);
    }
}
