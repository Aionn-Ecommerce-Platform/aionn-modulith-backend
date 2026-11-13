package com.aionn.ucp.application.order;

import java.time.Clock;
import java.util.HashMap;

import com.aionn.contracts.port.catalog.CatalogQueryPort;
import com.aionn.contracts.port.ordering.OrderSnapshotQueryPort;
import com.aionn.ucp.adapter.rest.dto.cart.response.UcpItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.cart.response.UcpTotalResponse;
import com.aionn.ucp.adapter.rest.dto.order.UcpOrderModels.*;
import com.aionn.ucp.application.UcpProtocolConstants;
import com.aionn.ucp.application.policy.UcpProtocolPolicy;
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

/** Application service for the UCP Order capability (dev.ucp.shopping.order). */
@Service
public class UcpOrderApplicationService {

    public static final String PROTOCOL_VERSION = UcpProtocolConstants.VERSION;
    public static final String ORDER_SCHEMA_URI = UcpProtocolConstants.ORDER_SCHEMA_URI;

    private final OrderSnapshotQueryPort orderSnapshotPort;
    private final CatalogQueryPort catalogQueryPort;
    private final UcpCheckoutSessionPort sessionPort;
    private final UcpSchemaValidationPort schemaValidator;
    private final UcpProtocolPolicy protocolPolicy;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    @Autowired
    public UcpOrderApplicationService(
            OrderSnapshotQueryPort orderSnapshotPort,
            CatalogQueryPort catalogQueryPort,
            UcpCheckoutSessionPort sessionPort,
            @Autowired(required = false) UcpSchemaValidationPort schemaValidator,
            UcpProtocolPolicy protocolPolicy) {
        this(orderSnapshotPort, catalogQueryPort, sessionPort, schemaValidator, protocolPolicy,
                Clock.systemUTC());
    }

    public UcpOrderApplicationService(
            OrderSnapshotQueryPort orderSnapshotPort,
            CatalogQueryPort catalogQueryPort,
            UcpCheckoutSessionPort sessionPort,
            UcpSchemaValidationPort schemaValidator,
            UcpProtocolPolicy protocolPolicy,
            Clock clock) {
        this.orderSnapshotPort = orderSnapshotPort;
        this.catalogQueryPort = catalogQueryPort;
        this.sessionPort = sessionPort;
        this.schemaValidator = schemaValidator;
        this.protocolPolicy = protocolPolicy;
        this.clock = clock;
        this.objectMapper = new ObjectMapper();
    }

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
        String shippingMethodLabel = session.map(this::resolveShippingMethodLabel).orElse("Standard Delivery");

        List<UcpOrderLineItemDto> lineItems = mapLineItems(snapshot);
        UcpOrderFulfillmentDto fulfillment = buildFulfillment(orderId, snapshot, lineItems, destination,
                shippingMethodLabel);
        List<UcpTotalResponse> totals = buildTotals(snapshot);

        String label = "Order #" + orderId;
        String permalinkUrl = protocolPolicy.storefrontBaseUrl() + "/orders/" + orderId;

        UcpOrderResponse response = new UcpOrderResponse(
                UcpResponseMetadata.success(UcpProtocolConstants.VERSION),
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
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()) {
            if (!authenticatedUserId.equals(orderUserId)) {
                throw new UcpProtocolException(403, "forbidden", "Access denied to order", "error", "$.id");
            }
            return;
        }
        // Platform/agent credentials have no buyer identity. Guest checkouts made by
        // an agent are therefore readable only through that guest namespace.
        if (orderUserId == null || !orderUserId.startsWith("ucp:guest:")) {
            throw new UcpProtocolException(403, "forbidden", "Access denied to order", "error", "$.id");
        }
    }

    private List<UcpOrderLineItemDto> mapLineItems(OrderSnapshotQueryPort.OrderSnapshot snapshot) {
        List<UcpOrderLineItemDto> lineItems = new ArrayList<>();
        boolean completed = isOrderCompleted(snapshot.status());
        String lineStatus = mapOrderStatusToLineStatus(snapshot.status());
        Map<String, CatalogQueryPort.ProductView> viewBySku = resolveProductViews(snapshot);

        for (OrderSnapshotQueryPort.OrderSnapshot.Line line : snapshot.lines()) {
            String lineId = "line_" + line.skuId();
            CatalogQueryPort.ProductView view = viewBySku.get(line.skuId());
            String title = resolveLineTitle(line.skuId(), view);
            String imageUrl = view != null && view.imageUrls() != null && !view.imageUrls().isEmpty()
                    ? view.imageUrls().get(0)
                    : null;
            UcpItemResponse item = new UcpItemResponse(line.skuId(), title, line.unitPriceMinor(), imageUrl);
            int fulfilled = completed ? line.qty() : 0;
            UcpOrderQuantityDto quantity = new UcpOrderQuantityDto(line.qty(), line.qty(), fulfilled);
            List<UcpTotalResponse> itemTotals = List.of(UcpTotalResponse.total(line.lineTotalMinor()));

            lineItems.add(new UcpOrderLineItemDto(lineId, item, quantity, itemTotals, lineStatus));
        }
        return lineItems;
    }

    /** Bulk-resolve display names for line SKUs; the snapshot port carries only skuIds. */
    private Map<String, CatalogQueryPort.ProductView> resolveProductViews(
            OrderSnapshotQueryPort.OrderSnapshot snapshot) {
        if (snapshot.lines() == null || snapshot.lines().isEmpty()) {
            return Map.of();
        }
        List<String> skuIds = snapshot.lines().stream()
                .map(OrderSnapshotQueryPort.OrderSnapshot.Line::skuId)
                .toList();
        try {
            CatalogQueryPort.LookupResult lookup = catalogQueryPort.lookupByProductOrSkuIds(skuIds);
            Map<String, CatalogQueryPort.ProductView> bySku = new HashMap<>();
            for (CatalogQueryPort.ProductView p : lookup.products()) {
                for (CatalogQueryPort.VariantView v : p.variants()) {
                    bySku.putIfAbsent(v.skuId(), p);
                }
            }
            return bySku;
        } catch (RuntimeException ex) {
            // Enrichment is best-effort: a catalog outage must not fail order read.
            return Map.of();
        }
    }

    /** Variant display name first, then product name, then the skuId. */
    private String resolveLineTitle(String skuId, CatalogQueryPort.ProductView view) {
        if (view == null) {
            return skuId;
        }
        if (view.variants() != null) {
            for (CatalogQueryPort.VariantView v : view.variants()) {
                if (skuId.equals(v.skuId()) && v.displayName() != null && !v.displayName().isBlank()) {
                    return v.displayName();
                }
            }
        }
        return view.name() != null && !view.name().isBlank() ? view.name() : skuId;
    }

    private UcpOrderFulfillmentDto buildFulfillment(
            String orderId,
            OrderSnapshotQueryPort.OrderSnapshot snapshot,
            List<UcpOrderLineItemDto> lineItems,
            UcpPostalAddressDto destination,
            String shippingMethodLabel) {
        List<UcpExpectationLineItemDto> expectationLines = lineItems.stream()
                .map(item -> new UcpExpectationLineItemDto(item.id(), item.quantity().total()))
                .toList();

        // No sourced delivery estimate yet, so use the order's own creation time.
        String promise = snapshot != null && snapshot.lines() != null && !snapshot.lines().isEmpty()
                ? clock.instant().toString()
                : null;
        UcpExpectationDto expectation = new UcpExpectationDto(
                "exp_" + orderId,
                expectationLines,
                "shipping",
                destination,
                shippingMethodLabel,
                promise);

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

    /** First {@code shipping} method's name/title from the session's UCP fulfillment block. */
    private String resolveShippingMethodLabel(UcpCheckoutSession session) {
        Map<String, Object> fulfillment = session.fulfillment();
        if (fulfillment == null) {
            return "Standard Delivery";
        }
        Object methodsObj = fulfillment.get("methods");
        if (!(methodsObj instanceof List<?> methods)) {
            return "Standard Delivery";
        }
        for (Object o : methods) {
            if (!(o instanceof Map<?, ?> method)) {
                continue;
            }
            Object type = method.get("type");
            if (!(type instanceof String t) || !"shipping".equalsIgnoreCase(t)) {
                continue;
            }
            Object name = method.get("name");
            if (name instanceof String s && !s.isBlank()) {
                return s;
            }
            Object title = method.get("title");
            if (title instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return "Standard Delivery";
    }

    private void validateResponseSchema(UcpOrderResponse response) {
        if (schemaValidator == null) {
            return;
        }
        try {
            schemaValidator.validate(UcpProtocolConstants.ORDER_SCHEMA_URI, objectMapper.valueToTree(response));
        } catch (UcpProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new UcpProtocolException(500, "schema_validation_failed",
                    "Order response schema validation failed: " + e.getMessage(), "error", null);
        }
    }
}
