package com.aionn.ucp.application.order;

import com.aionn.sharedkernel.integration.port.ordering.OrderSnapshotQueryPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderSnapshotQueryPort.OrderSnapshot;
import com.aionn.sharedkernel.integration.port.ordering.OrderSnapshotQueryPort.OrderSnapshot.Line;
import com.aionn.ucp.adapter.rest.dto.order.UcpOrderModels.UcpOrderResponse;
import com.aionn.ucp.application.port.out.UcpCheckoutSessionPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.aionn.ucp.domain.model.UcpCheckoutSession;
import com.aionn.ucp.infrastructure.schema.PinnedUcpSchemaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UcpOrderApplicationServiceTest {

        @Mock
        private OrderSnapshotQueryPort orderSnapshotPort;

        @Mock
        private UcpCheckoutSessionPort sessionPort;

        private PinnedUcpSchemaValidator schemaValidator;
        private UcpOrderApplicationService orderService;

        @BeforeEach
        void setUp() {
                schemaValidator = new PinnedUcpSchemaValidator();
                orderService = new UcpOrderApplicationService(orderSnapshotPort, sessionPort, schemaValidator);
        }

        @Test
        void getOrderSuccessfullyValidatesAgainstPinnedSchema() {
                String orderId = "ord_1001";
                String userId = "user_42";

                OrderSnapshot snapshot = new OrderSnapshot(
                                orderId,
                                userId,
                                "merch_1",
                                "VND",
                                "COMPLETED",
                                50000L,
                                10000L,
                                60000L,
                                List.of(new Line("sku_prod_1", 2, 25000L, 50000L)));

                UcpCheckoutSession session = new UcpCheckoutSession(
                                "chk_12345",
                                userId,
                                null,
                                "completed",
                                "VND",
                                Map.of(),
                                Map.of(
                                                "first_name", "Nguyen",
                                                "last_name", "An",
                                                "phone_number", "+84901234567",
                                                "postal_address", Map.of(
                                                                "street_address", "123 Le Loi",
                                                                "address_locality", "District 1",
                                                                "address_region", "Ho Chi Minh",
                                                                "address_country", "VN",
                                                                "postal_code", "700000")),
                                null,
                                orderId,
                                Instant.now(),
                                Instant.now(),
                                Instant.now().plusSeconds(3600));

                when(orderSnapshotPort.findOrderById(orderId)).thenReturn(Optional.of(snapshot));
                when(sessionPort.findByOrderId(orderId)).thenReturn(Optional.of(session));

                UcpOrderResponse response = orderService.getOrder(orderId, userId);

                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo(orderId);
                assertThat(response.checkoutId()).isEqualTo("chk_12345");
                assertThat(response.currency()).isEqualTo("VND");
                assertThat(response.permalinkUrl()).isEqualTo("https://aionn.vn/orders/" + orderId);
                assertThat(response.lineItems()).hasSize(1);
                assertThat(response.lineItems().get(0).status()).isEqualTo("fulfilled");
                assertThat(response.lineItems().get(0).quantity().total()).isEqualTo(2);
                assertThat(response.lineItems().get(0).quantity().fulfilled()).isEqualTo(2);
                assertThat(response.fulfillment().expectations()).hasSize(1);
                assertThat(response.fulfillment().expectations().get(0).destination().streetAddress())
                                .isEqualTo("123 Le Loi");
                assertThat(response.totals()).hasSize(3); // subtotal, shipping, total
        }

        @Test
        void getOrderWithoutCheckoutSessionFallbackToDefaults() {
                String orderId = "ord_2002";
                String userId = "user_99";

                OrderSnapshot snapshot = new OrderSnapshot(
                                orderId,
                                userId,
                                "merch_1",
                                "USD",
                                "PROCESSING",
                                2000L,
                                0L,
                                2000L,
                                List.of(new Line("sku_shoe", 1, 2000L, 2000L)));

                when(orderSnapshotPort.findOrderById(orderId)).thenReturn(Optional.of(snapshot));
                when(sessionPort.findByOrderId(orderId)).thenReturn(Optional.empty());

                UcpOrderResponse response = orderService.getOrder(orderId, userId);

                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo(orderId);
                assertThat(response.checkoutId()).isEqualTo("chk_" + orderId);
                assertThat(response.lineItems().get(0).status()).isEqualTo("processing");
                assertThat(response.lineItems().get(0).quantity().fulfilled()).isEqualTo(0);
                assertThat(response.totals()).hasSize(2); // subtotal, total (shipping is 0)
        }

        @Test
        void getOrderCancelledMapsLineStatusToRemoved() {
                String orderId = "ord_3003";

                OrderSnapshot snapshot = new OrderSnapshot(
                                orderId,
                                null,
                                "merch_1",
                                "USD",
                                "CANCELLED",
                                1000L,
                                0L,
                                1000L,
                                List.of(new Line("sku_item", 1, 1000L, 1000L)));

                when(orderSnapshotPort.findOrderById(orderId)).thenReturn(Optional.of(snapshot));
                when(sessionPort.findByOrderId(orderId)).thenReturn(Optional.empty());

                UcpOrderResponse response = orderService.getOrder(orderId, null);

                assertThat(response.lineItems().get(0).status()).isEqualTo("removed");
        }

        @Test
        void getOrderThrows400WhenOrderIdIsBlank() {
                assertThatThrownBy(() -> orderService.getOrder("  ", "user_1"))
                                .isInstanceOf(UcpProtocolException.class)
                                .hasMessageContaining("Order ID must not be blank")
                                .satisfies(e -> assertThat(((UcpProtocolException) e).getStatusCode()).isEqualTo(400));
        }

        @Test
        void getOrderThrows404WhenOrderNotFound() {
                when(orderSnapshotPort.findOrderById("ord_unknown")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> orderService.getOrder("ord_unknown", "user_1"))
                                .isInstanceOf(UcpProtocolException.class)
                                .hasMessageContaining("Order not found")
                                .satisfies(e -> assertThat(((UcpProtocolException) e).getStatusCode()).isEqualTo(404));
        }

        @Test
        void getOrderThrows403WhenCallerDoesNotOwnOrder() {
                OrderSnapshot snapshot = new OrderSnapshot(
                                "ord_1",
                                "user_owner",
                                "merch_1",
                                "USD",
                                "PENDING",
                                100L,
                                0L,
                                100L,
                                List.of(new Line("sku_1", 1, 100L, 100L)));

                when(orderSnapshotPort.findOrderById("ord_1")).thenReturn(Optional.of(snapshot));

                assertThatThrownBy(() -> orderService.getOrder("ord_1", "user_different"))
                                .isInstanceOf(UcpProtocolException.class)
                                .hasMessageContaining("Access denied")
                                .satisfies(e -> assertThat(((UcpProtocolException) e).getStatusCode()).isEqualTo(403));
        }
}
