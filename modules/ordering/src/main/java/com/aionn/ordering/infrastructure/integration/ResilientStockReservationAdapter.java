package com.aionn.ordering.infrastructure.integration;

import com.aionn.ordering.application.port.out.StockReservationGateway;
import com.aionn.ordering.application.port.out.observability.OrderingMetricsPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@Primary
@Order(0)
public class ResilientStockReservationAdapter implements StockReservationGateway {

    private static final String INSTANCE = "ordering-inventory";

    private final com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort inventoryStockReservation;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final OrderingMetricsPort metrics;

    public ResilientStockReservationAdapter(
            com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort inventoryStockReservation,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            OrderingMetricsPort metrics) {
        this.inventoryStockReservation = inventoryStockReservation;
        this.retry = retryRegistry.retry(INSTANCE);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        this.metrics = metrics;
    }

    @Override
    public List<Reservation> reserveAll(String orderId, List<ReservationLine> lines, int ttlSeconds) {
        Supplier<List<Reservation>> action = () -> {
            try {
                List<com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort.ReservationLine> mapped = lines.stream()
                        .map(l -> new com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort.ReservationLine(
                                l.skuId(), l.warehouseId(), l.qty(), l.unitPrice(), l.currency()))
                        .toList();
                List<com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort.Reservation> result =
                        inventoryStockReservation.reserveAll(orderId, mapped, ttlSeconds);
                return result.stream()
                        .map(r -> new Reservation(r.reservationId(), r.skuId(), r.warehouseId(),
                                r.qty(), r.unitPrice(), r.currency()))
                        .toList();
            } catch (com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort.ReservationException ex) {
                throw new ReservationException(ex.getSkuId(), ex.getMessage());
            }
        };
        try {
            List<Reservation> result = Retry.decorateSupplier(retry,
                    CircuitBreaker.decorateSupplier(circuitBreaker, action)).get();
            metrics.gatewayOutcome("inventory", "success");
            return result;
        } catch (ReservationException re) {
            metrics.gatewayOutcome("inventory", "reservation_failed");
            throw re;
        } catch (RuntimeException ex) {
            metrics.gatewayOutcome("inventory", "failure");
            log.error("Inventory reservation gateway failed: {}", ex.getMessage());
            throw ex;
        }
    }

    @Override
    public void commit(String reservationId) {
        execute("commit", () -> {
            inventoryStockReservation.commit(reservationId);
            return null;
        });
    }

    @Override
    public void release(String reservationId, String reason) {
        execute("release", () -> {
            inventoryStockReservation.release(reservationId, reason);
            return null;
        });
    }

    private <T> T execute(String operation, Supplier<T> action) {
        Supplier<T> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, action));
        try {
            T result = decorated.get();
            metrics.gatewayOutcome("inventory", "success");
            return result;
        } catch (RuntimeException ex) {
            metrics.gatewayOutcome("inventory", "failure");
            log.error("Inventory gateway {} failed: {}", operation, ex.getMessage());
            throw ex;
        }
    }
}
