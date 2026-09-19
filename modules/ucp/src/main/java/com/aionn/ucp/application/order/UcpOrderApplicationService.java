package com.aionn.ucp.application.order;

import com.aionn.sharedkernel.integration.port.ordering.OrderSnapshotQueryPort;
import com.aionn.ucp.adapter.rest.dto.cart.UcpItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.cart.UcpTotalResponse;
import com.aionn.ucp.adapter.rest.dto.order.UcpOrderModels.*;
import com.aionn.ucp.application.port.out.UcpCheckoutSessionPort;
import com.aionn.ucp.application.port.out.UcpSchemaValidationPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.aionn.ucp.domain.model.UcpCheckoutSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Application service for UCP Order capability (dev.ucp.shopping.order).
 * Coordinates order lookup, ownership validation, checkout reconciliation,
 * and canonical schema validation without domain leakage.
 */
@Service
public class UcpOrderApplicationService {

    public static final String PROTOCOL_VERSION = "2026-08-25";
    public static final String ORDER_SCHEMA_URI = "https://ucp.dev/schemas/shopping/order.json";
    private static final String BASE_STORE_URL = "https://aionn.vn";

    private final OrderSnapshotQueryPort orderSnapshotPort;
    private final UcpCheckoutSessionPort sessionPort;
    private final UcpSchemaValidationPort schemaValidator;
    private final ObjectMapper objectMapper;
    private final String version;

    @Autowired
    public UcpOrderApplicationService(
            OrderSnapshotQueryPort orderSnapshotPort,
            UcpCheckoutSessionPort sessionPort,
            @Autowired(required = false) UcpSchemaValidationPort schemaValidator) {
        this.orderSnapshotPort = orderSnapshotPort;
        this.sessionPort = sessionPort;
        this.schemaValidator = schemaValidator;
        this.objectMapper = new ObjectMapper();
        this.version = PROTOCOL_VERSION;
    }

    /**
     * Retrieves an order by its unique identifier, verifying ownership and
     * assembling
     * a conforming UCP order response.
     *
     * @param orderId             the order identifier
     * @param authenticatedUserId the authenticated user ID (if present)
     * @return conforming UcpOrderResponse
     */
    public UcpOrderResponse getOrder(String orderId, String authenticatedUserId) {
        if (orderId == null || orderId.isBlank()) {
            throw new UcpProtocolException(400, "invalid_request", "Order ID must not be blank", "error", "$.id");
        }

        OrderSnapshotQueryPort.OrderSnapshot snapshot = orderSnapshotPort.findOrderById(orderId)
                .orElseThrow(() -> new UcpProtocolException(404, "item_not_found", "Order not found: " + orderId,
                        "error", "$.id"));

        verifyOwnership(snapshot.userId(), authenticatedUserId);

        Optional<UcpCheckoutSession> session = sessionPort.findByOrderId(orderId);
        String checkoutId = session.map(UcpCheckoutSession::id).orElse("chk_" + orderId);
        UcpPostalAddressDto destination = session.map(s -> mapBuyerAddress(s.buyer()))
                .orElseGet(this::emptyPostalAddress);

        List<UcpOrderLineItemDto> lineItems = mapLineItems(snapshot);
        UcpOrderFulfillmentDto fulfillment = buildFulfillment(orderId, snapshot, lineItems, destination);
        List<UcpTotalResponse> totals = buildTotals(snapshot);

        String label = "Order #" + orderId;
        String permalinkUrl = BASE_STORE_URL + "/orders/" + orderId;

        UcpOrderResponse response = new UcpOrderResponse(
                UcpResponseMetadata.success(version),
                snapshot.orderId(),
                label,
                checkoutId,
                permalinkUrl,
                lineItems,
                fulfillment,
                snapshot.currency(),
                totals,
                null,
                null,
                null,
                null);

        validateResponseSchema(response);
        return response;
    }

    private void verifyOwnership(String orderUserId, String authenticatedUserId) {
        if (orderUserId != null && authenticatedUserId != null && !authenticatedUserId.isBlank()
                && !orderUserId.equals(authenticatedUserId)) {
            throw new UcpProtocolException(403, "forbidden", "Access denied to order", "error", "$.id");
        }
    }

    private List<UcpOrderLineItemDto> mapLineItems(OrderSnapshotQueryPort.OrderSnapshot snapshot) {
        List<UcpOrderLineItemDto> lineItems = new ArrayList<>();
        boolean completed = isOrderCompleted(snapshot.status());
        String lineStatus = mapOrderStatusToLineStatus(snapshot.status());

        for (OrderSnapshotQueryPort.OrderSnapshot.Line line : snapshot.lines()) {
            String lineId = "line_" + line.skuId();
            UcpItemResponse item = new UcpItemResponse(line.skuId(), line.skuId(), line.unitPriceMinor(), null);
            int fulfilled = completed ? line.qty() : 0;
            UcpOrderQuantityDto quantity = new UcpOrderQuantityDto(line.qty(), line.qty(), fulfilled);
            List<UcpTotalResponse> itemTotals = List.of(UcpTotalResponse.total(line.lineTotalMinor()));

            lineItems.add(new UcpOrderLineItemDto(lineId, item, quantity, itemTotals, lineStatus));
        }
        return lineItems;
    }

    private UcpOrderFulfillmentDto buildFulfillment(
            String orderId,
            OrderSnapshotQueryPort.OrderSnapshot snapshot,
            List<UcpOrderLineItemDto> lineItems,
            UcpPostalAddressDto destination) {
        List<UcpExpectationLineItemDto> expectationLines = lineItems.stream()
                .map(item -> new UcpExpectationLineItemDto(item.id(), item.quantity().total()))
                .toList();

        UcpExpectationDto expectation = new UcpExpectationDto(
                "exp_" + orderId,
                expectationLines,
                "shipping",
                destination,
                "Standard Delivery",
                "now");

        return new UcpOrderFulfillmentDto(List.of(expectation), null);
    }

    private List<UcpTotalResponse> buildTotals(OrderSnapshotQueryPort.OrderSnapshot snapshot) {
        List<UcpTotalResponse> totals = new ArrayList<>();
        totals.add(UcpTotalResponse.subtotal(snapshot.subtotalMinor()));
        if (snapshot.shippingMinor() > 0) {
            totals.add(UcpTotalResponse.shipping(snapshot.shippingMinor()));
        }
        totals.add(UcpTotalResponse.total(snapshot.totalMinor()));
        return totals;
    }

    private boolean isOrderCompleted(String status) {
        return "DELIVERED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);
    }

    private String mapOrderStatusToLineStatus(String status) {
        if (status == null) {
            return "processing";
        }
        return switch (status.toUpperCase()) {
            case "DELIVERED", "COMPLETED" -> "fulfilled";
            case "CANCELLED", "REFUNDED" -> "removed";
            default -> "processing";
        };
    }

    private UcpPostalAddressDto mapBuyerAddress(Map<String, Object> buyer) {
        if (buyer == null) {
            return emptyPostalAddress();
        }
        String firstName = buyer.get("first_name") instanceof String s ? s : null;
        String lastName = buyer.get("last_name") instanceof String s ? s : null;
        String phone = buyer.get("phone_number") instanceof String s ? s : null;

        if (buyer.get("postal_address") instanceof Map<?, ?> addr) {
            return new UcpPostalAddressDto(
                    addr.get("street_address") instanceof String s ? s : null,
                    addr.get("extended_address") instanceof String s ? s : null,
                    addr.get("address_locality") instanceof String s ? s : null,
                    addr.get("address_region") instanceof String s ? s : null,
                    addr.get("address_country") instanceof String s ? s : null,
                    addr.get("postal_code") instanceof String s ? s : null,
                    firstName,
                    lastName,
                    phone);
        }
        return new UcpPostalAddressDto(null, null, null, null, null, null, firstName, lastName, phone);
    }

    private UcpPostalAddressDto emptyPostalAddress() {
        return new UcpPostalAddressDto(null, null, null, null, null, null, null, null, null);
    }

    private void validateResponseSchema(UcpOrderResponse response) {
        if (schemaValidator == null) {
            return;
        }
        try {
            schemaValidator.validate(ORDER_SCHEMA_URI, objectMapper.valueToTree(response));
        } catch (UcpProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new UcpProtocolException(500, "schema_validation_failed",
                    "Order response schema validation failed: " + e.getMessage(), "error", null);
        }
    }
}
