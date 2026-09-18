package com.aionn.ucp.application.checkout;

import com.aionn.sharedkernel.integration.port.catalog.CatalogQueryPort;
import com.aionn.sharedkernel.integration.port.catalog.PricingQueryPort;
import com.aionn.sharedkernel.integration.port.ordering.CartOperationsPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderPlacementPort;
import com.aionn.ucp.adapter.rest.dto.cart.UcpItemRequest;
import com.aionn.ucp.adapter.rest.dto.cart.UcpLineItemRequest;
import com.aionn.ucp.adapter.rest.dto.checkout.UcpCheckoutRequest;
import com.aionn.ucp.adapter.rest.dto.checkout.UcpCheckoutResponse;
import com.aionn.ucp.application.port.out.UcpCheckoutSessionPort;
import com.aionn.ucp.application.port.out.UcpSchemaValidationPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.aionn.ucp.domain.model.UcpCheckoutSession;
import com.aionn.ucp.infrastructure.persistence.InMemoryUcpCheckoutSessionAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UcpCheckoutApplicationServiceTest {

    private UcpCheckoutSessionPort sessionPort;

    @Mock
    private CartOperationsPort cartPort;

    @Mock
    private PricingQueryPort pricingPort;

    @Mock
    private CatalogQueryPort catalogQueryPort;

    @Mock
    private OrderPlacementPort orderPlacementPort;

    @Mock
    private UcpSchemaValidationPort schemaValidator;

    private Clock clock;
    private UcpCheckoutApplicationService service;

    private final Instant now = Instant.parse("2026-09-18T10:00:00Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(now, ZoneId.of("UTC"));
        sessionPort = new InMemoryUcpCheckoutSessionAdapter();
        service = new UcpCheckoutApplicationService(
                sessionPort,
                cartPort,
                pricingPort,
                catalogQueryPort,
                orderPlacementPort,
                schemaValidator,
                clock);
    }

    @Test
    void createCheckoutFromLineItemsSuccessfully() {
        UcpCheckoutRequest request = new UcpCheckoutRequest(
                null,
                List.of(new UcpLineItemRequest("li_1", new UcpItemRequest("sku-1"), 2)),
                Map.of("name", "John Doe"),
                null);

        PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                "sku-1", "m-1", BigDecimal.valueOf(10.00), "USD", true);
        when(pricingPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1", pricing));

        CatalogQueryPort.ProductView productView = new CatalogQueryPort.ProductView(
                "p-1", "Test Product", "Desc", List.of("https://img.com/1.jpg"), List.of());
        when(catalogQueryPort.findByProductOrSkuId("sku-1")).thenReturn(Optional.of(productView));

        UcpCheckoutResponse response = service.createCheckout(request, "user-1");

        assertThat(response).isNotNull();
        assertThat(response.id()).startsWith("chk_");
        assertThat(response.status()).isEqualTo("incomplete");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.lineItems()).hasSize(1);
        assertThat(response.totals().get(0).amount()).isEqualTo(2000L);
        assertThat(response.links()).hasSize(2);
        verify(schemaValidator).validate(any(), any());
    }

    @Test
    void createCheckoutFromCartIdConversionSuccessfully() {
        CartOperationsPort.CartSnapshot cart = new CartOperationsPort.CartSnapshot(
                "cart-100", "user-1", Map.of("sku-1", 1), null, now, now);
        when(cartPort.findCartById("cart-100")).thenReturn(Optional.of(cart));

        PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                "sku-1", "m-1", BigDecimal.valueOf(15.00), "USD", true);
        when(pricingPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1", pricing));

        UcpCheckoutRequest request = new UcpCheckoutRequest("cart-100", null, null, null);
        UcpCheckoutResponse response = service.createCheckout(request, "user-1");

        assertThat(response).isNotNull();
        assertThat(response.id()).startsWith("chk_");
        assertThat(response.lineItems()).hasSize(1);
        assertThat(response.totals().get(0).amount()).isEqualTo(1500L);
    }

    @Test
    void createCheckoutFromCartIdReusesIncompleteSession() {
        CartOperationsPort.CartSnapshot cart = new CartOperationsPort.CartSnapshot(
                "cart-200", "user-2", Map.of("sku-1", 1), null, now, now);
        when(cartPort.findCartById("cart-200")).thenReturn(Optional.of(cart));

        PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                "sku-1", "m-1", BigDecimal.valueOf(15.00), "USD", true);
        when(pricingPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1", pricing));

        // Create first session
        UcpCheckoutResponse first = service.createCheckout(new UcpCheckoutRequest("cart-200", null, null, null), "user-2");

        // Second creation for same cart should reuse incomplete session
        UcpCheckoutResponse second = service.createCheckout(new UcpCheckoutRequest("cart-200", null, null, null), "user-2");

        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    void createCheckoutThrowsWhenCartNotFound() {
        when(cartPort.findCartById("cart-none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createCheckout(new UcpCheckoutRequest("cart-none", null, null, null), "user-1"))
                .isInstanceOf(UcpProtocolException.class)
                .satisfies(ex -> {
                    UcpProtocolException ucpEx = (UcpProtocolException) ex;
                    assertThat(ucpEx.getStatusCode()).isEqualTo(404);
                    assertThat(ucpEx.getErrorCode()).isEqualTo("cart_not_found");
                });
    }

    @Test
    void createCheckoutThrowsWhenCartAccessDeniedForDifferentUser() {
        CartOperationsPort.CartSnapshot cart = new CartOperationsPort.CartSnapshot(
                "cart-owned", "user-owner", Map.of("sku-1", 1), null, now, now);
        when(cartPort.findCartById("cart-owned")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> service.createCheckout(new UcpCheckoutRequest("cart-owned", null, null, null), "user-intruder"))
                .isInstanceOf(UcpProtocolException.class)
                .satisfies(ex -> {
                    UcpProtocolException ucpEx = (UcpProtocolException) ex;
                    assertThat(ucpEx.getStatusCode()).isEqualTo(403);
                    assertThat(ucpEx.getErrorCode()).isEqualTo("access_denied");
                });
    }

    @Test
    void createCheckoutThrowsWhenCartEmpty() {
        CartOperationsPort.CartSnapshot cart = new CartOperationsPort.CartSnapshot(
                "cart-empty", "user-1", Map.of(), null, now, now);
        when(cartPort.findCartById("cart-empty")).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> service.createCheckout(new UcpCheckoutRequest("cart-empty", null, null, null), "user-1"))
                .isInstanceOf(UcpProtocolException.class)
                .satisfies(ex -> {
                    UcpProtocolException ucpEx = (UcpProtocolException) ex;
                    assertThat(ucpEx.getStatusCode()).isEqualTo(400);
                    assertThat(ucpEx.getErrorCode()).isEqualTo("cart_empty");
                });
    }

    @Test
    void createCheckoutThrowsWhenMixedCurrencies() {
        UcpCheckoutRequest request = new UcpCheckoutRequest(
                null,
                List.of(
                        new UcpLineItemRequest("li_1", new UcpItemRequest("sku-usd"), 1),
                        new UcpLineItemRequest("li_2", new UcpItemRequest("sku-vnd"), 1)),
                null,
                null);

        PricingQueryPort.SkuPricing pUsd = new PricingQueryPort.SkuPricing("sku-usd", "m-1", BigDecimal.TEN, "USD", true);
        PricingQueryPort.SkuPricing pVnd = new PricingQueryPort.SkuPricing("sku-vnd", "m-1", BigDecimal.valueOf(100000), "VND", true);
        when(pricingPort.resolvePricing(List.of("sku-usd", "sku-vnd"))).thenReturn(Map.of("sku-usd", pUsd, "sku-vnd", pVnd));

        assertThatThrownBy(() -> service.createCheckout(request, "user-1"))
                .isInstanceOf(UcpProtocolException.class)
                .satisfies(ex -> {
                    UcpProtocolException ucpEx = (UcpProtocolException) ex;
                    assertThat(ucpEx.getStatusCode()).isEqualTo(400);
                    assertThat(ucpEx.getErrorCode()).isEqualTo("mixed_currency_not_supported");
                });
    }

    @Test
    void createCheckoutThrowsWhenQuantityOverflows() {
        UcpCheckoutRequest request = new UcpCheckoutRequest(
                null,
                List.of(
                        new UcpLineItemRequest("li_1", new UcpItemRequest("sku-1"), Integer.MAX_VALUE),
                        new UcpLineItemRequest("li_2", new UcpItemRequest("sku-1"), 1)),
                null,
                null);

        assertThatThrownBy(() -> service.createCheckout(request, "user-1"))
                .isInstanceOf(UcpProtocolException.class)
                .satisfies(ex -> {
                    UcpProtocolException ucpEx = (UcpProtocolException) ex;
                    assertThat(ucpEx.getStatusCode()).isEqualTo(400);
                    assertThat(ucpEx.getErrorCode()).isEqualTo("invalid_quantity");
                });
    }

    @Test
    void getCheckoutSuccessfully() {
        UcpCheckoutSession session = new UcpCheckoutSession(
                "chk-1", "user-1", null, "incomplete", "USD", Map.of("sku-1", 1), null, null, null, now, now, now.plusSeconds(3600));
        sessionPort.save(session);

        PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                "sku-1", "m-1", BigDecimal.valueOf(25.00), "USD", true);
        when(pricingPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1", pricing));

        UcpCheckoutResponse response = service.getCheckout("chk-1", "user-1");

        assertThat(response.id()).isEqualTo("chk-1");
        assertThat(response.totals().get(0).amount()).isEqualTo(2500L);
    }

    @Test
    void getCheckoutThrowsWhenAccessDenied() {
        UcpCheckoutSession session = new UcpCheckoutSession(
                "chk-1", "user-1", null, "incomplete", "USD", Map.of(), null, null, null, now, now, now.plusSeconds(3600));
        sessionPort.save(session);

        assertThatThrownBy(() -> service.getCheckout("chk-1", "user-other"))
                .isInstanceOf(UcpProtocolException.class)
                .satisfies(ex -> {
                    UcpProtocolException ucpEx = (UcpProtocolException) ex;
                    assertThat(ucpEx.getStatusCode()).isEqualTo(403);
                    assertThat(ucpEx.getErrorCode()).isEqualTo("access_denied");
                });
    }

    @Test
    void updateCheckoutSuccessfully() {
        UcpCheckoutSession session = new UcpCheckoutSession(
                "chk-upd", "user-1", null, "incomplete", "USD", Map.of("sku-1", 1), null, null, null, now, now, now.plusSeconds(3600));
        sessionPort.save(session);

        UcpCheckoutRequest request = new UcpCheckoutRequest(
                null,
                List.of(new UcpLineItemRequest("li_1", new UcpItemRequest("sku-2"), 3)),
                Map.of("email", "buyer@test.com"),
                null);

        PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                "sku-2", "m-1", BigDecimal.valueOf(30.00), "USD", true);
        when(pricingPort.resolvePricing(List.of("sku-2"))).thenReturn(Map.of("sku-2", pricing));

        UcpCheckoutResponse response = service.updateCheckout("chk-upd", request, "user-1");

        assertThat(response.id()).isEqualTo("chk-upd");
        assertThat(response.lineItems().get(0).item().id()).isEqualTo("sku-2");
        assertThat(response.totals().get(0).amount()).isEqualTo(9000L);
    }

    @Test
    void completeCheckoutPlacesOrderAndReturnsCompletedSession() {
        UcpCheckoutSession session = new UcpCheckoutSession(
                "chk-complete", "user-1", null, "incomplete", "USD", Map.of("sku-1", 2), null, null, null, now, now, now.plusSeconds(3600));
        sessionPort.save(session);

        PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                "sku-1", "m-1", BigDecimal.valueOf(10.00), "USD", true);
        when(pricingPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1", pricing));

        OrderPlacementPort.PlacedOrder placedOrder = new OrderPlacementPort.PlacedOrder("order-999", 2000L, "USD", "CONFIRMED");
        when(orderPlacementPort.placeHeadless(any())).thenReturn(placedOrder);

        UcpCheckoutResponse response = service.completeCheckout("chk-complete", "user-1");

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.order()).isNotNull();
        assertThat(response.order().id()).isEqualTo("order-999");
    }

    @Test
    void cancelCheckoutTransitionsStatusToCanceled() {
        UcpCheckoutSession session = new UcpCheckoutSession(
                "chk-cancel", "user-1", null, "incomplete", "USD", Map.of("sku-1", 1), null, null, null, now, now, now.plusSeconds(3600));
        sessionPort.save(session);

        PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                "sku-1", "m-1", BigDecimal.valueOf(10.00), "USD", true);
        when(pricingPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1", pricing));

        UcpCheckoutResponse response = service.cancelCheckout("chk-cancel", "user-1");

        assertThat(response.status()).isEqualTo("canceled");
    }
}
