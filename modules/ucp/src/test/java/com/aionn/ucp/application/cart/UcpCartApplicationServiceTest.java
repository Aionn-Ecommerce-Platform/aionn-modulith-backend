package com.aionn.ucp.application.cart;

import com.aionn.sharedkernel.integration.port.catalog.CatalogQueryPort;
import com.aionn.sharedkernel.integration.port.catalog.PricingQueryPort;
import com.aionn.sharedkernel.integration.port.ordering.CartOperationsPort;
import com.aionn.ucp.adapter.rest.dto.cart.UcpCartRequest;
import com.aionn.ucp.adapter.rest.dto.cart.UcpCartResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpItemRequest;
import com.aionn.ucp.adapter.rest.dto.cart.UcpLineItemRequest;
import com.aionn.ucp.application.port.out.UcpSchemaValidationPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UcpCartApplicationServiceTest {

        @Mock
        private CartOperationsPort cartPort;

        @Mock
        private PricingQueryPort pricingPort;

        @Mock
        private CatalogQueryPort catalogQueryPort;

        @Mock
        private UcpSchemaValidationPort schemaValidator;

        private Clock clock;
        private UcpCartApplicationService service;

        private final Instant now = Instant.parse("2026-09-18T10:00:00Z");

        @BeforeEach
        void setUp() {
                clock = Clock.fixed(now, ZoneId.of("UTC"));
                service = new UcpCartApplicationService(cartPort, pricingPort, catalogQueryPort, schemaValidator,
                                clock);
        }

        @Test
        void createCartSuccessfullyBuildsAndValidatesResponse() {
                UcpCartRequest request = new UcpCartRequest(
                                List.of(new UcpLineItemRequest("client_li_1", new UcpItemRequest("sku-100"), 2)),
                                null,
                                null);

                PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                                "sku-100", "merchant-1", BigDecimal.valueOf(19.99), "USD", true);
                when(pricingPort.resolvePricing(List.of("sku-100"))).thenReturn(Map.of("sku-100", pricing));

                CatalogQueryPort.ProductView productView = new CatalogQueryPort.ProductView(
                                "prod-1", "Premium Running Shoes", "Comfortable sneakers",
                                List.of("https://img.example/shoe.jpg"), List.of());
                when(catalogQueryPort.findByProductOrSkuId("sku-100")).thenReturn(Optional.of(productView));

                CartOperationsPort.CartSnapshot savedSnapshot = new CartOperationsPort.CartSnapshot(
                                "cart_01jtestcart", "ucp:guest:cart_01jtestcart", Map.of("sku-100", 2), null, now, now);
                when(cartPort.saveCartItems(anyString(), anyString(), any())).thenReturn(savedSnapshot);

                UcpCartResponse response = service.createCart(request, null);

                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo("cart_01jtestcart");
                assertThat(response.currency()).isEqualTo("USD");
                assertThat(response.lineItems()).hasSize(1);
                assertThat(response.lineItems().get(0).item().title()).isEqualTo("Premium Running Shoes");
                assertThat(response.lineItems().get(0).item().price()).isEqualTo(1999L);
                assertThat(response.lineItems().get(0).quantity()).isEqualTo(2);
                assertThat(response.lineItems().get(0).totals().get(0).amount()).isEqualTo(3998L);
                assertThat(response.totals().get(0).amount()).isEqualTo(3998L);
                verify(schemaValidator).validate(eq(UcpCartApplicationService.CART_SCHEMA_URI), any());
        }

        @Test
        void createCartWithVndCurrencyCalculatesMinorUnitsWithoutScaling() {
                UcpCartRequest request = new UcpCartRequest(
                                List.of(new UcpLineItemRequest(null, new UcpItemRequest("sku-vnd"), 1)),
                                null,
                                null);

                PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                                "sku-vnd", "merchant-1", BigDecimal.valueOf(50000), "VND", true);
                when(pricingPort.resolvePricing(List.of("sku-vnd"))).thenReturn(Map.of("sku-vnd", pricing));
                when(catalogQueryPort.findByProductOrSkuId("sku-vnd")).thenReturn(Optional.empty());

                CartOperationsPort.CartSnapshot savedSnapshot = new CartOperationsPort.CartSnapshot(
                                "cart_vnd", "user_123", Map.of("sku-vnd", 1), null, now, now);
                when(cartPort.saveCartItems(anyString(), eq("user_123"), any())).thenReturn(savedSnapshot);

                UcpCartResponse response = service.createCart(request, "user_123");

                assertThat(response.currency()).isEqualTo("VND");
                assertThat(response.lineItems().get(0).item().price()).isEqualTo(50000L);
                assertThat(response.totals().get(0).amount()).isEqualTo(50000L);
        }

        @Test
        void createCartRejectsEmptyLineItems() {
                UcpCartRequest request = new UcpCartRequest(List.of(), null, null);

                assertThatThrownBy(() -> service.createCart(request, null))
                                .isInstanceOf(UcpProtocolException.class)
                                .satisfies(ex -> {
                                        UcpProtocolException ucpEx = (UcpProtocolException) ex;
                                        assertThat(ucpEx.getStatusCode()).isEqualTo(400);
                                        assertThat(ucpEx.getErrorCode()).isEqualTo("invalid_request");
                                });
        }

        @Test
        void createCartRejectsBlankItemId() {
                UcpCartRequest request = new UcpCartRequest(
                                List.of(new UcpLineItemRequest(null, new UcpItemRequest("  "), 1)),
                                null,
                                null);

                assertThatThrownBy(() -> service.createCart(request, null))
                                .isInstanceOf(UcpProtocolException.class)
                                .satisfies(ex -> {
                                        UcpProtocolException ucpEx = (UcpProtocolException) ex;
                                        assertThat(ucpEx.getStatusCode()).isEqualTo(400);
                                        assertThat(ucpEx.getErrorCode()).isEqualTo("invalid_item");
                                });
        }

        @Test
        void createCartRejectsNonPositiveQuantity() {
                UcpCartRequest request = new UcpCartRequest(
                                List.of(new UcpLineItemRequest(null, new UcpItemRequest("sku-1"), 0)),
                                null,
                                null);

                assertThatThrownBy(() -> service.createCart(request, null))
                                .isInstanceOf(UcpProtocolException.class)
                                .satisfies(ex -> {
                                        UcpProtocolException ucpEx = (UcpProtocolException) ex;
                                        assertThat(ucpEx.getStatusCode()).isEqualTo(400);
                                        assertThat(ucpEx.getErrorCode()).isEqualTo("invalid_quantity");
                                });
        }

        @Test
        void createCartRejectsInactiveOrMissingItem() {
                UcpCartRequest request = new UcpCartRequest(
                                List.of(new UcpLineItemRequest(null, new UcpItemRequest("sku-missing"), 1)),
                                null,
                                null);

                when(pricingPort.resolvePricing(List.of("sku-missing"))).thenReturn(Map.of());

                assertThatThrownBy(() -> service.createCart(request, null))
                                .isInstanceOf(UcpProtocolException.class)
                                .satisfies(ex -> {
                                        UcpProtocolException ucpEx = (UcpProtocolException) ex;
                                        assertThat(ucpEx.getStatusCode()).isEqualTo(400);
                                        assertThat(ucpEx.getErrorCode()).isEqualTo("item_not_found");
                                });
        }

        @Test
        void getCartReturnsExistingCart() {
                CartOperationsPort.CartSnapshot snapshot = new CartOperationsPort.CartSnapshot(
                                "cart-abc", "user-abc", Map.of("sku-1", 3), null, now, now);
                when(cartPort.findCartById("cart-abc")).thenReturn(Optional.of(snapshot));

                PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                                "sku-1", "merchant-1", BigDecimal.valueOf(10.00), "USD", true);
                when(pricingPort.resolvePricing(List.of("sku-1"))).thenReturn(Map.of("sku-1", pricing));

                UcpCartResponse response = service.getCart("cart-abc", "user-abc");

                assertThat(response.id()).isEqualTo("cart-abc");
                assertThat(response.lineItems()).hasSize(1);
                assertThat(response.totals().get(0).amount()).isEqualTo(3000L);
        }

        @Test
        void getCartThrowsWhenNotFound() {
                when(cartPort.findCartById("cart-unknown")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.getCart("cart-unknown", null))
                                .isInstanceOf(UcpProtocolException.class)
                                .satisfies(ex -> {
                                        UcpProtocolException ucpEx = (UcpProtocolException) ex;
                                        assertThat(ucpEx.getStatusCode()).isEqualTo(404);
                                        assertThat(ucpEx.getErrorCode()).isEqualTo("cart_not_found");
                                });
        }

        @Test
        void updateCartReplacesItemsSuccessfully() {
                CartOperationsPort.CartSnapshot existing = new CartOperationsPort.CartSnapshot(
                                "cart-upd", "user-upd", Map.of("old-sku", 1), null, now, now);
                when(cartPort.findCartById("cart-upd")).thenReturn(Optional.of(existing));

                UcpCartRequest request = new UcpCartRequest(
                                List.of(new UcpLineItemRequest(null, new UcpItemRequest("new-sku"), 4)),
                                null,
                                null);

                PricingQueryPort.SkuPricing pricing = new PricingQueryPort.SkuPricing(
                                "new-sku", "merchant-1", BigDecimal.valueOf(5.00), "USD", true);
                when(pricingPort.resolvePricing(List.of("new-sku"))).thenReturn(Map.of("new-sku", pricing));

                CartOperationsPort.CartSnapshot updated = new CartOperationsPort.CartSnapshot(
                                "cart-upd", "user-upd", Map.of("new-sku", 4), null, now, now);
                when(cartPort.saveCartItems(eq("cart-upd"), eq("user-upd"), any())).thenReturn(updated);

                UcpCartResponse response = service.updateCart("cart-upd", request, "user-upd");

                assertThat(response.id()).isEqualTo("cart-upd");
                assertThat(response.lineItems()).hasSize(1);
                assertThat(response.lineItems().get(0).item().id()).isEqualTo("new-sku");
                assertThat(response.totals().get(0).amount()).isEqualTo(2000L);
        }

        @Test
        void cancelCartClearsItems() {
                CartOperationsPort.CartSnapshot existing = new CartOperationsPort.CartSnapshot(
                                "cart-cancel", "user-cancel", Map.of("sku-1", 1), null, now, now);
                when(cartPort.findCartById("cart-cancel")).thenReturn(Optional.of(existing));

                CartOperationsPort.CartSnapshot cleared = new CartOperationsPort.CartSnapshot(
                                "cart-cancel", "user-cancel", Map.of(), null, now, now);
                when(cartPort.clearCart("cart-cancel", "user-cancel")).thenReturn(cleared);

                UcpCartResponse response = service.cancelCart("cart-cancel", "user-cancel");

                assertThat(response.id()).isEqualTo("cart-cancel");
                assertThat(response.lineItems()).isEmpty();
                assertThat(response.totals().get(0).amount()).isEqualTo(0L);
        }
}
