package com.aionn.ucp.application.checkout;

import com.aionn.sharedkernel.integration.port.catalog.CatalogQueryPort;
import com.aionn.sharedkernel.integration.port.catalog.PricingQueryPort;
import com.aionn.sharedkernel.integration.port.ordering.CartOperationsPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderPlacementPort;
import com.aionn.sharedkernel.util.IdGenerator;
import com.aionn.ucp.adapter.rest.dto.cart.UcpItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpLineItemRequest;
import com.aionn.ucp.adapter.rest.dto.cart.UcpLineItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.cart.UcpTotalResponse;
import com.aionn.ucp.adapter.rest.dto.checkout.UcpCheckoutRequest;
import com.aionn.ucp.adapter.rest.dto.checkout.UcpCheckoutResponse;
import com.aionn.ucp.adapter.rest.dto.checkout.UcpLinkResponse;
import com.aionn.ucp.adapter.rest.dto.checkout.UcpOrderConfirmationResponse;
import com.aionn.ucp.application.port.out.UcpCheckoutSessionPort;
import com.aionn.ucp.application.port.out.UcpSchemaValidationPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.aionn.ucp.domain.model.UcpCheckoutSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UcpCheckoutApplicationService {

    private static final String PROTOCOL_VERSION = "2026-08-25";
    private static final String CHECKOUT_SCHEMA_URI = "https://ucp.dev/schemas/shopping/checkout.json";
    private static final long CHECKOUT_TTL_HOURS = 6;
    private static final String BASE_STORE_URL = "https://aionn.vn";

    private final UcpCheckoutSessionPort sessionPort;
    private final CartOperationsPort cartPort;
    private final PricingQueryPort pricingPort;
    private final CatalogQueryPort catalogQueryPort;
    private final OrderPlacementPort orderPlacementPort;
    private final UcpSchemaValidationPort schemaValidator;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UcpCheckoutApplicationService(
            UcpCheckoutSessionPort sessionPort,
            CartOperationsPort cartPort,
            PricingQueryPort pricingPort,
            CatalogQueryPort catalogQueryPort,
            OrderPlacementPort orderPlacementPort,
            UcpSchemaValidationPort schemaValidator,
            Clock clock) {
        this.sessionPort = sessionPort;
        this.cartPort = cartPort;
        this.pricingPort = pricingPort;
        this.catalogQueryPort = catalogQueryPort;
        this.orderPlacementPort = orderPlacementPort;
        this.schemaValidator = schemaValidator;
        this.clock = clock;
    }

    public UcpCheckoutResponse createCheckout(UcpCheckoutRequest request, String authenticatedUserId) {
        Instant now = clock.instant();
        String userId = (authenticatedUserId != null && !authenticatedUserId.isBlank())
                ? authenticatedUserId
                : "ucp:guest:" + IdGenerator.ulid();

        Map<String, Integer> items;
        String cartId = null;

        if (request != null && request.cartId() != null && !request.cartId().isBlank()) {
            cartId = request.cartId();
            CartOperationsPort.CartSnapshot cart = cartPort.findCartById(cartId)
                    .orElseThrow(() -> new UcpProtocolException(404, "cart_not_found", "Cart not found: " + request.cartId(), "error", "$.cart_id"));
            verifyOwnership(cart.userId(), authenticatedUserId, "$.cart_id");

            if (cart.items() == null || cart.items().isEmpty()) {
                throw new UcpProtocolException(400, "cart_empty", "Cannot convert empty cart to checkout", "error", "$.cart_id");
            }

            // Reuse existing incomplete checkout session for the same cart to prevent duplicate conflicting sessions
            Optional<UcpCheckoutSession> existingSession = sessionPort.findIncompleteByCartId(cartId);
            if (existingSession.isPresent() && !existingSession.get().isExpired(now)) {
                UcpCheckoutSession session = existingSession.get();
                verifyOwnership(session.userId(), authenticatedUserId, "$.cart_id");
                String resolvedCurrency = validateItemsAndDetermineCurrency(cart.items());
                UcpCheckoutSession updated = session.withUpdatedItems(cart.items(), request.buyer(), request.context(), resolvedCurrency, now);
                sessionPort.save(updated);
                UcpCheckoutResponse response = buildCheckoutResponse(updated);
                validateResponseSchema(response);
                return response;
            }

            items = cart.items();
        } else {
            if (request == null || request.lineItems() == null || request.lineItems().isEmpty()) {
                throw new UcpProtocolException(400, "invalid_request", "Checkout must contain line_items or a valid cart_id", "error", "$.line_items");
            }
            items = extractSkuQuantities(request.lineItems());
        }

        String currency = validateItemsAndDetermineCurrency(items);

        String checkoutId = "chk_" + IdGenerator.ulid();
        Instant expiresAt = now.plus(CHECKOUT_TTL_HOURS, ChronoUnit.HOURS);

        UcpCheckoutSession session = new UcpCheckoutSession(
                checkoutId,
                userId,
                cartId,
                "incomplete",
                currency,
                items,
                request != null ? request.buyer() : null,
                request != null ? request.context() : null,
                null,
                now,
                now,
                expiresAt);

        sessionPort.save(session);

        UcpCheckoutResponse response = buildCheckoutResponse(session);
        validateResponseSchema(response);
        return response;
    }

    public UcpCheckoutResponse getCheckout(String checkoutId, String authenticatedUserId) {
        UcpCheckoutSession session = sessionPort.findById(checkoutId)
                .orElseThrow(() -> new UcpProtocolException(404, "checkout_not_found", "Checkout session not found: " + checkoutId, "error", "$.id"));
        verifyOwnership(session.userId(), authenticatedUserId, "$.id");

        UcpCheckoutResponse response = buildCheckoutResponse(session);
        validateResponseSchema(response);
        return response;
    }

    public UcpCheckoutResponse updateCheckout(String checkoutId, UcpCheckoutRequest request, String authenticatedUserId) {
        Instant now = clock.instant();
        UcpCheckoutSession existing = sessionPort.findById(checkoutId)
                .orElseThrow(() -> new UcpProtocolException(404, "checkout_not_found", "Checkout session not found: " + checkoutId, "error", "$.id"));
        verifyOwnership(existing.userId(), authenticatedUserId, "$.id");

        if (existing.isCompleted() || existing.isCanceled() || existing.isExpired(now)) {
            throw new UcpProtocolException(400, "invalid_state",
                    "Cannot update checkout session in status: " + existing.status(), "error", "$.status");
        }

        if (request == null || request.lineItems() == null || request.lineItems().isEmpty()) {
            throw new UcpProtocolException(400, "invalid_request", "Checkout must contain at least one line item", "error", "$.line_items");
        }

        Map<String, Integer> items = extractSkuQuantities(request.lineItems());
        String currency = validateItemsAndDetermineCurrency(items);

        UcpCheckoutSession updated = existing.withUpdatedItems(
                items,
                request.buyer() != null ? request.buyer() : existing.buyer(),
                request.context() != null ? request.context() : existing.context(),
                currency,
                now);

        sessionPort.save(updated);

        UcpCheckoutResponse response = buildCheckoutResponse(updated);
        validateResponseSchema(response);
        return response;
    }

    public UcpCheckoutResponse completeCheckout(String checkoutId, String authenticatedUserId) {
        Instant now = clock.instant();
        UcpCheckoutSession existing = sessionPort.findById(checkoutId)
                .orElseThrow(() -> new UcpProtocolException(404, "checkout_not_found", "Checkout session not found: " + checkoutId, "error", "$.id"));
        verifyOwnership(existing.userId(), authenticatedUserId, "$.id");

        if (existing.isCompleted()) {
            UcpCheckoutResponse response = buildCheckoutResponse(existing);
            validateResponseSchema(response);
            return response;
        }

        if (existing.isCanceled() || existing.isExpired(now)) {
            throw new UcpProtocolException(400, "invalid_state",
                    "Cannot complete checkout session in status: " + existing.status(), "error", "$.status");
        }

        List<OrderPlacementPort.PlaceCommand.Line> orderLines = existing.items().entrySet().stream()
                .map(e -> new OrderPlacementPort.PlaceCommand.Line(e.getKey(), e.getValue()))
                .toList();

        OrderPlacementPort.PlaceCommand command = new OrderPlacementPort.PlaceCommand(
                existing.userId(),
                orderLines,
                null,
                "cod",
                existing.currency(),
                null,
                checkoutId);

        OrderPlacementPort.PlacedOrder placedOrder = orderPlacementPort.placeHeadless(command);

        UcpCheckoutSession completed = existing.withCompleted(placedOrder.orderId(), now);
        sessionPort.save(completed);

        UcpCheckoutResponse response = buildCheckoutResponse(completed);
        validateResponseSchema(response);
        return response;
    }

    public UcpCheckoutResponse cancelCheckout(String checkoutId, String authenticatedUserId) {
        Instant now = clock.instant();
        UcpCheckoutSession existing = sessionPort.findById(checkoutId)
                .orElseThrow(() -> new UcpProtocolException(404, "checkout_not_found", "Checkout session not found: " + checkoutId, "error", "$.id"));
        verifyOwnership(existing.userId(), authenticatedUserId, "$.id");

        if (existing.isCompleted()) {
            throw new UcpProtocolException(400, "invalid_state", "Cannot cancel completed checkout session", "error", "$.status");
        }

        UcpCheckoutSession canceled = existing.withCanceled(now);
        sessionPort.save(canceled);

        UcpCheckoutResponse response = buildCheckoutResponse(canceled);
        validateResponseSchema(response);
        return response;
    }

    private void verifyOwnership(String resourceUserId, String authenticatedUserId, String path) {
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()) {
            if (!authenticatedUserId.equals(resourceUserId)) {
                throw new UcpProtocolException(403, "access_denied", "You are not authorized to access this checkout session", "error", path);
            }
        } else if (!resourceUserId.startsWith("ucp:guest:")) {
            throw new UcpProtocolException(403, "access_denied", "You are not authorized to access this checkout session", "error", path);
        }
    }

    private String validateItemsAndDetermineCurrency(Map<String, Integer> items) {
        if (items == null || items.isEmpty()) {
            throw new UcpProtocolException(400, "invalid_request", "Items cannot be empty", "error", "$.line_items");
        }

        List<String> skuIds = new ArrayList<>(items.keySet());
        Map<String, PricingQueryPort.SkuPricing> pricingMap = pricingPort.resolvePricing(skuIds);

        String commonCurrency = null;
        for (int i = 0; i < skuIds.size(); i++) {
            String skuId = skuIds.get(i);
            PricingQueryPort.SkuPricing pricing = pricingMap.get(skuId);
            if (pricing == null || !pricing.active()) {
                throw new UcpProtocolException(400, "item_not_found", "Item not found or inactive: " + skuId, "error",
                        "$.line_items[" + i + "].item.id");
            }
            if (pricing.currency() != null) {
                if (commonCurrency != null && !commonCurrency.equalsIgnoreCase(pricing.currency())) {
                    throw new UcpProtocolException(400, "mixed_currency_not_supported",
                            "All checkout items must use the same currency. Found: " + commonCurrency + " and " + pricing.currency(),
                            "error", "$.line_items[" + i + "]");
                }
                commonCurrency = pricing.currency();
            }
        }
        return commonCurrency != null ? commonCurrency : "USD";
    }

    private Map<String, Integer> extractSkuQuantities(List<UcpLineItemRequest> lineItems) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (int i = 0; i < lineItems.size(); i++) {
            UcpLineItemRequest item = lineItems.get(i);
            if (item == null || item.item() == null || item.item().id() == null || item.item().id().isBlank()) {
                throw new UcpProtocolException(400, "invalid_item", "Line item ID must not be blank", "error",
                        "$.line_items[" + i + "].item.id");
            }
            if (item.quantity() == null || item.quantity() < 1) {
                throw new UcpProtocolException(400, "invalid_quantity", "Quantity must be at least 1", "error",
                        "$.line_items[" + i + "].quantity");
            }

            String skuId = item.item().id();
            int qty = item.quantity();
            quantities.compute(skuId, (k, current) -> {
                if (current == null) {
                    return qty;
                }
                long sum = (long) current + (long) qty;
                if (sum > Integer.MAX_VALUE) {
                    throw new UcpProtocolException(400, "invalid_quantity",
                            "Aggregated quantity for item " + skuId + " exceeds maximum allowed",
                            "error", "$.line_items");
                }
                return (int) sum;
            });
        }
        return quantities;
    }

    private UcpCheckoutResponse buildCheckoutResponse(UcpCheckoutSession session) {
        List<String> skuIds = new ArrayList<>(session.items().keySet());
        Map<String, PricingQueryPort.SkuPricing> pricingMap = skuIds.isEmpty() ? Map.of()
                : pricingPort.resolvePricing(skuIds);

        List<UcpLineItemResponse> lineItemResponses = new ArrayList<>();
        long calculatedSubtotal = 0;
        int itemIndex = 1;

        for (Map.Entry<String, Integer> entry : session.items().entrySet()) {
            String skuId = entry.getKey();
            int qty = entry.getValue();
            PricingQueryPort.SkuPricing pricing = pricingMap.get(skuId);

            String title = "Product Item";
            String imageUrl = null;
            var productViewOpt = catalogQueryPort.findByProductOrSkuId(skuId);
            if (productViewOpt.isPresent()) {
                var productView = productViewOpt.get();
                title = productView.name();
                if (productView.imageUrls() != null && !productView.imageUrls().isEmpty()) {
                    imageUrl = productView.imageUrls().get(0);
                }
            }

            long unitPriceMinor = 0;
            if (pricing != null) {
                unitPriceMinor = toMinorUnits(pricing.price(), session.currency());
            }

            long itemTotal = unitPriceMinor * qty;
            calculatedSubtotal += itemTotal;

            UcpItemResponse itemResponse = new UcpItemResponse(skuId, title, unitPriceMinor, imageUrl);
            List<UcpTotalResponse> itemTotals = List.of(
                    UcpTotalResponse.subtotal(itemTotal),
                    UcpTotalResponse.total(itemTotal));

            lineItemResponses.add(new UcpLineItemResponse(
                    "li_" + itemIndex++,
                    itemResponse,
                    qty,
                    itemTotals));
        }

        List<UcpTotalResponse> checkoutTotals = List.of(
                UcpTotalResponse.subtotal(calculatedSubtotal),
                UcpTotalResponse.total(calculatedSubtotal));

        List<UcpLinkResponse> links = List.of(
                UcpLinkResponse.termsOfService(BASE_STORE_URL),
                UcpLinkResponse.privacyPolicy(BASE_STORE_URL));

        UcpOrderConfirmationResponse orderConfirmation = session.orderId() != null
                ? UcpOrderConfirmationResponse.of(session.orderId(), BASE_STORE_URL)
                : null;

        UcpResponseMetadata ucpMetadata = UcpResponseMetadata.success(PROTOCOL_VERSION);

        return new UcpCheckoutResponse(
                ucpMetadata,
                session.id(),
                session.status(),
                session.currency(),
                lineItemResponses,
                checkoutTotals,
                links,
                session.buyer(),
                session.context(),
                orderConfirmation,
                session.expiresAt() != null ? session.expiresAt().toString() : null,
                null);
    }

    private long toMinorUnits(BigDecimal price, String currency) {
        if (price == null) {
            return 0;
        }
        if ("VND".equalsIgnoreCase(currency)) {
            return price.longValue();
        }
        return price.multiply(BigDecimal.valueOf(100)).longValue();
    }

    private void validateResponseSchema(UcpCheckoutResponse response) {
        if (schemaValidator == null) {
            return;
        }
        try {
            schemaValidator.validate(CHECKOUT_SCHEMA_URI, objectMapper.valueToTree(response));
        } catch (UcpProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new UcpProtocolException(500, "schema_validation_failed",
                    "Response schema validation error: " + e.getMessage(), "error", null);
        }
    }
}
