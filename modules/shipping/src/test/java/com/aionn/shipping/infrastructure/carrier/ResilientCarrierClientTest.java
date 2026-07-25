package com.aionn.shipping.infrastructure.carrier;

import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.observability.ShippingMetricsPort;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResilientCarrierClientTest {

    @Mock
    private CarrierClient delegate;

    @Mock
    private RetryRegistry retryRegistry;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private ShippingMetricsPort metrics;

    private ResilientCarrierClient resilientClient;

    @BeforeEach
    void setUp() {
        when(retryRegistry.retry("shipping-carrier")).thenReturn(Retry.ofDefaults("shipping-carrier"));
        when(circuitBreakerRegistry.circuitBreaker("shipping-carrier")).thenReturn(CircuitBreaker.ofDefaults("shipping-carrier"));

        resilientClient = new ResilientCarrierClient(
                List.of(delegate),
                retryRegistry,
                circuitBreakerRegistry,
                metrics
        );
    }

    @Test
    void quoteDelegatesAndRecordsSuccess() {
        var address = new ShipmentAddress("John", "0912", "Addr", "W", "D", "P", "VN");
        var dims = new ShipmentDimensions(100, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
        var expected = new CarrierClient.Quote(BigDecimal.valueOf(20000), "VND", "HN", "details", null, null);

        when(delegate.quote(address, dims, "VND")).thenReturn(expected);

        var result = resilientClient.quote(address, dims, "VND");

        assertThat(result).isEqualTo(expected);
        verify(metrics).carrierOutcome("quote", "success");
    }

    @Test
    void quoteFailsAndRecordsFailure() {
        var address = new ShipmentAddress("John", "0912", "Addr", "W", "D", "P", "VN");
        var dims = new ShipmentDimensions(100, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);

        when(delegate.quote(address, dims, "VND")).thenThrow(new RuntimeException("API error"));

        assertThatThrownBy(() -> resilientClient.quote(address, dims, "VND"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("API error");

        verify(metrics).carrierOutcome("quote", "failure");
    }

    @Test
    void registerDelegatesAndRecordsSuccess() {
        var address = new ShipmentAddress("John", "0912", "Addr", "W", "D", "P", "VN");
        var dims = new ShipmentDimensions(100, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN);
        var expected = new CarrierClient.Registration("TR_1", "C_1", null);

        when(delegate.register("S_1", "O_1", address, dims, BigDecimal.ZERO, BigDecimal.valueOf(20000), "VND"))
                .thenReturn(expected);

        var result = resilientClient.register("S_1", "O_1", address, dims, BigDecimal.ZERO, BigDecimal.valueOf(20000), "VND");

        assertThat(result).isEqualTo(expected);
        verify(metrics).carrierOutcome("register", "success");
    }

    @Test
    void fetchLabelDelegatesAndRecordsSuccess() {
        when(delegate.fetchLabel("TR_1")).thenReturn("label-url");

        String result = resilientClient.fetchLabel("TR_1");

        assertThat(result).isEqualTo("label-url");
        verify(metrics).carrierOutcome("fetchLabel", "success");
    }

    @Test
    void cancelDelegatesAndRecordsSuccess() {
        doNothing().when(delegate).cancel("TR_1", "reason");

        resilientClient.cancel("TR_1", "reason");

        verify(metrics).carrierOutcome("cancel", "success");
    }

    @Test
    void fetchOrderDetailDelegatesAndRecordsSuccess() {
        var expected = new CarrierClient.OrderDetail(
                "DELIVERED", null, null, null, null, null, null, null);
        when(delegate.fetchOrderDetail("TR_1")).thenReturn(expected);

        var result = resilientClient.fetchOrderDetail("TR_1");

        assertThat(result).isEqualTo(expected);
        verify(metrics).carrierOutcome("fetchOrderDetail", "success");
    }
}
