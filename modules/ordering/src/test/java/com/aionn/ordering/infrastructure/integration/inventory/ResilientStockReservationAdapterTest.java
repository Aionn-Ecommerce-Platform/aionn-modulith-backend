package com.aionn.ordering.infrastructure.integration.inventory;

import com.aionn.ordering.application.port.out.StockReservationGateway;
import com.aionn.ordering.application.port.out.observability.OrderingMetricsPort;
import com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResilientStockReservationAdapterTest {

    @Mock
    private InventoryStockReservationPort inventoryStockReservation;

    @Mock
    private OrderingMetricsPort metrics;

    private ResilientStockReservationAdapter adapter;

    @BeforeEach
    void setUp() {
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        adapter = new ResilientStockReservationAdapter(
                inventoryStockReservation, retryRegistry, circuitBreakerRegistry, metrics);
    }

    @Test
    void reserveAllDelegatesAndMapsResultOnSuccess() {
        StockReservationGateway.ReservationLine line = new StockReservationGateway.ReservationLine(
                "sku-1", "wh-1", 2, BigDecimal.TEN, "VND");
        InventoryStockReservationPort.ReservationLine kernelLine = new InventoryStockReservationPort.ReservationLine(
                "sku-1", "wh-1", 2, BigDecimal.TEN, "VND");
        InventoryStockReservationPort.Reservation kernelRes = new InventoryStockReservationPort.Reservation(
                "res-1", "sku-1", "wh-1", 2, BigDecimal.TEN, "VND");

        when(inventoryStockReservation.reserveAll("ord-1", List.of(kernelLine), 300))
                .thenReturn(List.of(kernelRes));

        List<StockReservationGateway.Reservation> result =
                adapter.reserveAll("ord-1", List.of(line), 300);

        assertEquals(1, result.size());
        assertEquals("res-1", result.get(0).reservationId());
        verify(metrics).gatewayOutcome("inventory", "success");
    }

    @Test
    void commitDelegatesAndRecordsMetricsOnSuccess() {
        adapter.commit("res-1");

        verify(inventoryStockReservation).commit("res-1");
        verify(metrics).gatewayOutcome("inventory", "success");
    }

    @Test
    void releaseDelegatesAndRecordsMetricsOnSuccess() {
        adapter.release("res-1", "cancelled");

        verify(inventoryStockReservation).release("res-1", "cancelled");
        verify(metrics).gatewayOutcome("inventory", "success");
    }

    @Test
    void reserveAllHandlesReservationExceptionAndRecordsFailureMetric() {
        StockReservationGateway.ReservationLine line = new StockReservationGateway.ReservationLine(
                "sku-1", "wh-1", 2, BigDecimal.TEN, "VND");
        InventoryStockReservationPort.ReservationLine kernelLine = new InventoryStockReservationPort.ReservationLine(
                "sku-1", "wh-1", 2, BigDecimal.TEN, "VND");

        doThrow(new InventoryStockReservationPort.ReservationException("sku-1", "wh-1", "Out of stock"))
                .when(inventoryStockReservation).reserveAll("ord-1", List.of(kernelLine), 300);

        assertThrows(StockReservationGateway.ReservationException.class, () ->
                adapter.reserveAll("ord-1", List.of(line), 300));

        verify(metrics).gatewayOutcome("inventory", "reservation_failed");
    }
}
