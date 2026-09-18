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
import com.aionn.ucp.domain.util.UcpCurrencyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class UcpCheckoutApplicationService {

    private static final String PROTOCOL_VERSION = "2026-08-25";
    private static final String CHECKOUT_SCHEMA_URI = "https://ucp.dev/schemas/shopping/checkout.json";
    private static final long CHECKOUT_TTL_HOURS = 6;
    private static final String BASE_STORE_URL = "https://aionn.vn";

    private static final String SEVERITY_ERROR = "error";
    private static final String CODE_CHECKOUT_NOT_FOUND = "checkout_not_found";
    private static final String MSG_CHECKOUT_NOT_FOUND = "Checkout session not found: ";
    private static final String PATH_CART_ID = "$.cart_id";
    private static final String PATH_LINE_ITEMS = "$.line_items";
    private static final String PATH_ID = "$.id";
    private static final String PATH_STATUS = "$.status";

    private final UcpCheckoutSessionPort sessionPort;
    private final CartOperationsPort cartPort;
    private final PricingQueryPort pricingPort;
    private final CatalogQueryPort catalogQueryPort;
    private final OrderPlacementPort orderPlacementPort;
    private final UcpSchemaValidationPort schemaValidator;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int STRIPE_COUNT = 256;
    private final Object[] sessionStripes = new Object[STRIPE_COUNT];
    private final Object[] cartStripes = new Object[STRIPE_COUNT];

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
        for (int i = 0; i < STRIPE_COUNT; i++) {
            sessionStripes[i] = new Object();
            cartStripes[i] = new Object();
        }
    }

    private Object getSessionLock(String checkoutId) {
        int hash = checkoutId != null ? checkoutId.hashCode() : 0;
        return sessionStripes[(hash & 0x7FFFFFFF) % STRIPE_COUNT];
    }

    private Object getCartLock(String cartId) {
        int hash = cartId != null ? cartId.hashCode() : 0;
        return cartStripes[(hash & 0x7FFFFFFF) % STRIPE_COUNT];
    }

    public UcpCheckoutResponse createCheckout(UcpCheckoutRequest request, String authenticatedUserId) {
        if (request == null) {
            throw new UcpProtocolException(400, "invalid_request", "Checkout request must not be null",
                    SEVERITY_ERROR, PATH_LINE_ITEMS);
        }

        Instant now = clock.instant();
        String userId = resolveUserId(authenticatedUserId);

        if (request.cartId() != null && !request.cartId().isBlank()) {
            return createOrReuseFromCart(request, userId, authenticatedUserId, now);
        }

        return createFromDirectItems(request, userId, now);
    }

    private String resolveUserId(String authenticatedUserId) {
        return (authenticatedUserId != null && !authenticatedUserId.isBlank())
                ? authenticatedUserId
                : "ucp:guest:" + IdGenerator.ulid();
    }

    private UcpCheckoutResponse createOrReuseFromCart(UcpCheckoutRequest request, String userId,
            String authenticatedUserId, Instant now) {
        String cartId = request.cartId();
        CartOperationsPort.CartSnapshot cart = cartPort.findCartById(cartId)
                .orElseThrow(() -> new UcpProtocolException(404, "cart_not_found",
                        "Cart not found: " + cartId, SEVERITY_ERROR, PATH_CART_ID));
        verifyOwnership(cart.userId(), authenticatedUserId, PATH_CART_ID);

        if (cart.items() == null || cart.items().isEmpty()) {
            throw new UcpProtocolException(400, "cart_empty", "Cannot convert empty cart to checkout",
                    SEVERITY_ERROR, PATH_CART_ID);
        }

        synchronized (getCartLock(cartId)) {
            // Reuse existing incomplete checkout session for the same cart to prevent duplicate conflicting sessions
            Optional<UcpCheckoutSession> existingSession = sessionPort.findIncompleteByCartId(cartId);
            if (existingSession.isPresent() && !existingSession.get().isExpired(now)) {
                UcpCheckoutSession session = existingSession.get();
                verifyOwnership(session.userId(), authenticatedUserId, PATH_CART_ID);
                ValidatedPricing validated = validateItemsAndDeterminePricing(cart.items());
                UcpCheckoutSession updated = session.withUpdatedItems(cart.items(), request.buyer(), request.context(),
                        validated.currency(), now, validated.priceSnapshot());
                boolean saved = sessionPort.updateIfMatches(updated, session.version(), session.status());
                if (!saved) {
                    throw new UcpProtocolException(409, "conflict",
                            "Checkout session was modified concurrently", SEVERITY_ERROR, PATH_CART_ID);
                }
                UcpCheckoutResponse response = buildCheckoutResponse(updated);
                validateResponseSchema(response);
                return response;
            }

            ValidatedPricing validated = validateItemsAndDeterminePricing(cart.items());
            String checkoutId = "chk_" + IdGenerator.ulid();
            Instant expiresAt = now.plus(CHECKOUT_TTL_HOURS, ChronoUnit.HOURS);

            UcpCheckoutSession session = new UcpCheckoutSession(
                    checkoutId,
                    userId,
                    cartId,
                    "incomplete",
                    validated.currency(),
                    cart.items(),
                    request.buyer(),
                    request.context(),
                    null,
                    now,
                    now,
                    expiresAt,
                    validated.priceSnapshot());

            sessionPort.save(session);
            UcpCheckoutResponse response = buildCheckoutResponse(session);
            validateResponseSchema(response);
            return response;
        }
    }

    private UcpCheckoutResponse createFromDirectItems(UcpCheckoutRequest request, String userId, Instant now) {
        if (request.lineItems() == null || request.lineItems().isEmpty()) {
            throw new UcpProtocolException(400, "invalid_request",
                    "Checkout must contain line_items or a valid cart_id", SEVERITY_ERROR, PATH_LINE_ITEMS);
        }

        Map<String, Integer> items = extractSkuQuantities(request.lineItems());
        ValidatedPricing validated = validateItemsAndDeterminePricing(items);

        String checkoutId = "chk_" + IdGenerator.ulid();
        Instant expiresAt = now.plus(CHECKOUT_TTL_HOURS, ChronoUnit.HOURS);

        UcpCheckoutSession session = new UcpCheckoutSession(
                checkoutId,
                userId,
                null,
                "incomplete",
                validated.currency(),
                items,
                request.buyer(),
                request.context(),
                null,
                now,
                now,
                expiresAt,
                validated.priceSnapshot());

        sessionPort.save(session);
        UcpCheckoutResponse response = buildCheckoutResponse(session);
        validateResponseSchema(response);
        return response;
    }

    public UcpCheckoutResponse getCheckout(String checkoutId, String authenticatedUserId) {
        UcpCheckoutSession session = sessionPort.findById(checkoutId)
                .orElseThrow(() -> new UcpProtocolException(404, CODE_CHECKOUT_NOT_FOUND,
                        MSG_CHECKOUT_NOT_FOUND + checkoutId, SEVERITY_ERROR, PATH_ID));
        verifyOwnership(session.userId(), authenticatedUserId, PATH_ID);

        UcpCheckoutResponse response = buildCheckoutResponse(session);
        validateResponseSchema(response);
        return response;
    }

    public UcpCheckoutResponse updateCheckout(String checkoutId, UcpCheckoutRequest request,
            String authenticatedUserId) {
        synchronized (getSessionLock(checkoutId)) {
            Instant now = clock.instant();
            UcpCheckoutSession existing = sessionPort.findById(checkoutId)
                    .orElseThrow(() -> new UcpProtocolException(404, CODE_CHECKOUT_NOT_FOUND,
                            MSG_CHECKOUT_NOT_FOUND + checkoutId, SEVERITY_ERROR, PATH_ID));
            verifyOwnership(existing.userId(), authenticatedUserId, PATH_ID);

            if (existing.isCompleted() || existing.isCanceled() || existing.isExpired(now)) {
                throw new UcpProtocolException(400, "invalid_state",
                        "Cannot update checkout session in status: " + existing.status(), SEVERITY_ERROR, PATH_STATUS);
            }

            if (request == null || request.lineItems() == null || request.lineItems().isEmpty()) {
                throw new UcpProtocolException(400, "invalid_request", "Checkout must contain at least one line item",
                        SEVERITY_ERROR, PATH_LINE_ITEMS);
            }

            Map<String, Integer> items = extractSkuQuantities(request.lineItems());
            ValidatedPricing validated = validateItemsAndDeterminePricing(items);

            UcpCheckoutSession updated = existing.withUpdatedItems(
                    items,
                    request.buyer() != null ? request.buyer() : existing.buyer(),
                    request.context() != null ? request.context() : existing.context(),
                    validated.currency(),
                    now,
                    validated.priceSnapshot());

            boolean saved = sessionPort.updateIfMatches(updated, existing.version(), existing.status());
            if (!saved) {
                throw new UcpProtocolException(409, "conflict",
                        "Checkout session was modified concurrently", SEVERITY_ERROR, PATH_STATUS);
            }

            UcpCheckoutResponse response = buildCheckoutResponse(updated);
            validateResponseSchema(response);
            return response;
        }
    }

    public UcpCheckoutResponse completeCheckout(String checkoutId, String authenticatedUserId) {
        synchronized (getSessionLock(checkoutId)) {
            Instant now = clock.instant();
            UcpCheckoutSession existing = sessionPort.findById(checkoutId)
                    .orElseThrow(() -> new UcpProtocolException(404, CODE_CHECKOUT_NOT_FOUND,
                            MSG_CHECKOUT_NOT_FOUND + checkoutId, SEVERITY_ERROR, PATH_ID));
            verifyOwnership(existing.userId(), authenticatedUserId, PATH_ID);

            if (existing.isCompleted()) {
                UcpCheckoutResponse response = buildCheckoutResponse(existing);
                validateResponseSchema(response);
                return response;
            }

            if (existing.isCanceled() || existing.isExpired(now)) {
                throw new UcpProtocolException(400, "invalid_state",
                        "Cannot complete checkout session in status: " + existing.status(), SEVERITY_ERROR, PATH_STATUS);
            }

            // Validate items and pricing before placing order
            ValidatedPricing validated = validateItemsAndDeterminePricing(existing.items());

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

            Map<String, Long> completedPriceSnapshot = placedOrder.linePrices() != null && !placedOrder.linePrices().isEmpty()
                    ? placedOrder.linePrices().entrySet().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> UcpCurrencyUtil.toMinorUnits(e.getValue(), placedOrder.currency()),
                                    (k1, k2) -> k1,
                                    java.util.LinkedHashMap::new))
                    : validated.priceSnapshot();

            UcpCheckoutSession completed = existing.withCompleted(placedOrder.orderId(), now, completedPriceSnapshot);
            boolean saved = sessionPort.updateIfMatches(completed, existing.version(), existing.status());
            if (!saved) {
                Optional<UcpCheckoutSession> latest = sessionPort.findById(checkoutId);
                if (latest.isPresent() && latest.get().isCompleted()) {
                    UcpCheckoutResponse response = buildCheckoutResponse(latest.get());
                    validateResponseSchema(response);
                    return response;
                }
                throw new UcpProtocolException(409, "conflict",
                        "Checkout session state changed concurrently during completion", SEVERITY_ERROR, PATH_STATUS);
            }

            UcpCheckoutResponse response = buildCheckoutResponse(completed);
            validateResponseSchema(response);
            return response;
        }
    }

    public UcpCheckoutResponse cancelCheckout(String checkoutId, String authenticatedUserId) {
        synchronized (getSessionLock(checkoutId)) {
            Instant now = clock.instant();
            UcpCheckoutSession existing = sessionPort.findById(checkoutId)
                    .orElseThrow(() -> new UcpProtocolException(404, CODE_CHECKOUT_NOT_FOUND,
                            MSG_CHECKOUT_NOT_FOUND + checkoutId, SEVERITY_ERROR, PATH_ID));
            verifyOwnership(existing.userId(), authenticatedUserId, PATH_ID);

            if (existing.isCompleted()) {
                throw new UcpProtocolException(400, "invalid_state", "Cannot cancel completed checkout session",
                        SEVERITY_ERROR, PATH_STATUS);
            }

            if (existing.isCanceled()) {
                UcpCheckoutResponse response = buildCheckoutResponse(existing);
                validateResponseSchema(response);
                return response;
            }

            Map<String, Long> priceSnapshot = existing.priceSnapshot();
            if (priceSnapshot == null || priceSnapshot.isEmpty()) {
                ValidatedPricing validated = validateItemsAndDeterminePricing(existing.items());
                priceSnapshot = validated.priceSnapshot();
            }

            UcpCheckoutSession canceled = existing.withCanceled(now, priceSnapshot);
            boolean saved = sessionPort.updateIfMatches(canceled, existing.version(), existing.status());
            if (!saved) {
                throw new UcpProtocolException(409, "conflict",
                        "Checkout session was modified concurrently", SEVERITY_ERROR, PATH_STATUS);
            }

            UcpCheckoutResponse response = buildCheckoutResponse(canceled);
            validateResponseSchema(response);
            return response;
        }
    }

    private void verifyOwnership(String resourceUserId, String authenticatedUserId, String path) {
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()) {
            if (!authenticatedUserId.equals(resourceUserId)) {
                throw new UcpProtocolException(403, "access_denied",
                        "You are not authorized to access this checkout session", SEVERITY_ERROR, path);
            }
        } else if (!resourceUserId.startsWith("ucp:guest:")) {
            throw new UcpProtocolException(403, "access_denied",
                    "You are not authorized to access this checkout session", SEVERITY_ERROR, path);
        }
    }

    private record ValidatedPricing(String currency, Map<String, Long> priceSnapshot) {
    }

    private ValidatedPricing validateItemsAndDeterminePricing(Map<String, Integer> items) {
        if (items == null || items.isEmpty()) {
            throw new UcpProtocolException(400, "invalid_request", "Items cannot be empty",
                    SEVERITY_ERROR, PATH_LINE_ITEMS);
        }

        List<String> skuIds = new ArrayList<>(items.keySet());
        Map<String, PricingQueryPort.SkuPricing> pricingMap = pricingPort.resolvePricing(skuIds);

        String commonCurrency = null;
        Map<String, Long> snapshot = new LinkedHashMap<>();

        for (int i = 0; i < skuIds.size(); i++) {
            String skuId = skuIds.get(i);
            PricingQueryPort.SkuPricing pricing = pricingMap.get(skuId);
            if (pricing == null || !pricing.active() || pricing.price() == null) {
                throw new UcpProtocolException(400, "item_not_found", "Item not found, inactive, or missing price: " + skuId,
                        SEVERITY_ERROR, "$.line_items[" + i + "].item.id");
            }
            if (pricing.currency() != null) {
                if (commonCurrency != null && !commonCurrency.equalsIgnoreCase(pricing.currency())) {
                    throw new UcpProtocolException(400, "mixed_currency_not_supported",
                            "All checkout items must use the same currency. Found: " + commonCurrency + " and "
                                    + pricing.currency(),
                            SEVERITY_ERROR, "$.line_items[" + i + "]");
                }
                commonCurrency = pricing.currency();
            }
            String resolvedCurrency = commonCurrency != null ? commonCurrency : "USD";
            snapshot.put(skuId, UcpCurrencyUtil.toMinorUnits(pricing.price(), resolvedCurrency));
        }
        return new ValidatedPricing(commonCurrency != null ? commonCurrency : "USD", snapshot);
    }

    private Map<String, Integer> extractSkuQuantities(List<UcpLineItemRequest> lineItems) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (int i = 0; i < lineItems.size(); i++) {
            UcpLineItemRequest item = lineItems.get(i);
            if (item == null || item.item() == null || item.item().id() == null || item.item().id().isBlank()) {
                throw new UcpProtocolException(400, "invalid_item", "Line item ID must not be blank",
                        SEVERITY_ERROR, "$.line_items[" + i + "].item.id");
            }
            if (item.quantity() == null || item.quantity() < 1) {
                throw new UcpProtocolException(400, "invalid_quantity", "Quantity must be at least 1",
                        SEVERITY_ERROR, "$.line_items[" + i + "].quantity");
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
                            SEVERITY_ERROR, PATH_LINE_ITEMS);
                }
                return (int) sum;
            });
        }
        return quantities;
    }

    private UcpCheckoutResponse buildCheckoutResponse(UcpCheckoutSession session) {
        List<String> skuIds = new ArrayList<>(session.items().keySet());
        Map<String, PricingQueryPort.SkuPricing> pricingMap = null;

        List<UcpLineItemResponse> lineItemResponses = new ArrayList<>();
        long calculatedSubtotal = 0;
        int itemIndex = 1;

        for (Map.Entry<String, Integer> entry : session.items().entrySet()) {
            String skuId = entry.getKey();
            int qty = entry.getValue();

            long unitPriceMinor;
            if (session.priceSnapshot() != null && session.priceSnapshot().containsKey(skuId)) {
                unitPriceMinor = session.priceSnapshot().get(skuId);
            } else {
                if (pricingMap == null) {
                    pricingMap = skuIds.isEmpty() ? Map.of() : pricingPort.resolvePricing(skuIds);
                }
                PricingQueryPort.SkuPricing pricing = pricingMap.get(skuId);
                if (pricing == null || !pricing.active() || pricing.price() == null) {
                    throw new UcpProtocolException(409, "item_not_found",
                            "Item no longer available: " + skuId, SEVERITY_ERROR, PATH_LINE_ITEMS);
                }
                unitPriceMinor = UcpCurrencyUtil.toMinorUnits(pricing.price(), session.currency());
            }

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
                    "Response schema validation error: " + e.getMessage(), SEVERITY_ERROR, null);
        }
    }
}
