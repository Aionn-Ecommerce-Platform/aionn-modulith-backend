package com.aionn.ucp.application.checkout;

import java.util.stream.Collectors;

import com.aionn.contracts.port.catalog.CatalogQueryPort;
import com.aionn.contracts.port.catalog.PricingQueryPort;
import com.aionn.contracts.port.identity.AddressLookupPort;
import com.aionn.contracts.port.ordering.CartOperationsPort;
import com.aionn.contracts.port.ordering.CheckoutQuotePort;
import com.aionn.contracts.port.ordering.OrderPlacementPort;
import com.aionn.sharedkernel.util.IdGenerator;
import com.aionn.ucp.adapter.rest.dto.cart.response.UcpItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.request.UcpLineItemRequest;
import com.aionn.ucp.adapter.rest.dto.cart.response.UcpLineItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.cart.response.UcpTotalResponse;
import com.aionn.ucp.adapter.rest.dto.checkout.request.UcpCheckoutRequest;
import com.aionn.ucp.adapter.rest.dto.checkout.response.UcpCheckoutResponse;
import com.aionn.ucp.adapter.rest.dto.checkout.response.UcpLinkResponse;
import com.aionn.ucp.adapter.rest.dto.checkout.response.UcpOrderConfirmationResponse;
import com.aionn.ucp.application.UcpProtocolConstants;
import com.aionn.ucp.application.policy.UcpProtocolPolicy;
import com.aionn.ucp.application.port.out.UcpCheckoutSessionPort;
import com.aionn.ucp.application.port.out.UcpSchemaValidationPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.aionn.ucp.domain.model.UcpCheckoutSession;
import com.aionn.ucp.domain.util.UcpCurrencyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UcpCheckoutApplicationService {

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
    private final UcpProtocolPolicy protocolPolicy;
    private final Clock clock;
    private final AddressLookupPort addressLookupPort;
    private final CheckoutQuotePort checkoutQuotePort;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int STRIPE_COUNT = 256;
    private static final int MAX_LINE_ITEMS = 100;
    private final Object[] sessionStripes = new Object[STRIPE_COUNT];
    private final Object[] cartStripes = new Object[STRIPE_COUNT];

    public UcpCheckoutApplicationService(
            UcpCheckoutSessionPort sessionPort,
            CartOperationsPort cartPort,
            PricingQueryPort pricingPort,
            CatalogQueryPort catalogQueryPort,
            OrderPlacementPort orderPlacementPort,
            UcpSchemaValidationPort schemaValidator,
            UcpProtocolPolicy protocolPolicy,
            Clock clock) {
        this(sessionPort, cartPort, pricingPort, catalogQueryPort, orderPlacementPort, schemaValidator,
                protocolPolicy, clock, null, null);
    }

    public UcpCheckoutApplicationService(
            UcpCheckoutSessionPort sessionPort,
            CartOperationsPort cartPort,
            PricingQueryPort pricingPort,
            CatalogQueryPort catalogQueryPort,
            OrderPlacementPort orderPlacementPort,
            UcpSchemaValidationPort schemaValidator,
            UcpProtocolPolicy protocolPolicy,
            Clock clock,
            AddressLookupPort addressLookupPort) {
        this(sessionPort, cartPort, pricingPort, catalogQueryPort, orderPlacementPort, schemaValidator,
                protocolPolicy, clock, addressLookupPort, null);
    }

    @Autowired
    public UcpCheckoutApplicationService(
            UcpCheckoutSessionPort sessionPort,
            CartOperationsPort cartPort,
            PricingQueryPort pricingPort,
            CatalogQueryPort catalogQueryPort,
            OrderPlacementPort orderPlacementPort,
            UcpSchemaValidationPort schemaValidator,
            UcpProtocolPolicy protocolPolicy,
            Clock clock,
            @Autowired(required = false) AddressLookupPort addressLookupPort,
            @Autowired(required = false) CheckoutQuotePort checkoutQuotePort) {
        this.sessionPort = sessionPort;
        this.cartPort = cartPort;
        this.pricingPort = pricingPort;
        this.catalogQueryPort = catalogQueryPort;
        this.orderPlacementPort = orderPlacementPort;
        this.schemaValidator = schemaValidator;
        this.protocolPolicy = protocolPolicy;
        this.clock = clock;
        this.addressLookupPort = addressLookupPort;
        this.checkoutQuotePort = checkoutQuotePort;
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
            // Reuse an incomplete session for the same cart to avoid conflicting sessions.
            Optional<UcpCheckoutSession> existingSession = sessionPort.findIncompleteByCartId(cartId);
            if (existingSession.isPresent() && !existingSession.get().isExpired(now)) {
                UcpCheckoutSession session = existingSession.get();
                verifyOwnership(session.userId(), authenticatedUserId, PATH_CART_ID);
                ValidatedPricing validated = validateItemsAndDeterminePricing(cart.items());
                String voucherCode = resolveVoucherCode(request, cart);
                UcpCheckoutSession updated = session.withUpdatedItems(cart.items(), request.buyer(), request.context(),
                        validated.currency(), now, validated.priceSnapshot(), request.payment(), request.fulfillment(),
                        voucherCode);
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
            Instant expiresAt = now.plus(protocolPolicy.checkoutTtl());
            String voucherCode = resolveVoucherCode(request, cart);

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
                    validated.priceSnapshot(),
                    request.payment(),
                    request.fulfillment(),
                    voucherCode,
                    1L);

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
        Instant expiresAt = now.plus(protocolPolicy.checkoutTtl());

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
                validated.priceSnapshot(),
                request.payment(),
                request.fulfillment(),
                request.voucherCode(),
                1L);

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

            if (existing.isCompleted() || existing.isCanceled() || existing.isCompleting() || existing.isExpired(now)) {
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
                    validated.priceSnapshot(),
                    request.payment() != null ? request.payment() : existing.payment(),
                    request.fulfillment() != null ? request.fulfillment() : existing.fulfillment(),
                    request.voucherCode() != null ? request.voucherCode() : existing.voucherCode());

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
        return completeCheckout(checkoutId, null, authenticatedUserId);
    }

    public UcpCheckoutResponse completeCheckout(String checkoutId, UcpCheckoutRequest request,
            String authenticatedUserId) {
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
                        "Cannot complete checkout session in status: " + existing.status(), SEVERITY_ERROR,
                        PATH_STATUS);
            }

            // Persist supplied fields before reserving the completing transition so the order matches.
            UcpCheckoutSession effective = mergeCompleteRequest(existing, request, now);

            ValidatedPricing validated = validateItemsAndDeterminePricing(effective.items());

            // Reserve a durable 'completing' state before invoking placeHeadless.
            UcpCheckoutSession completing = effective.isCompleting() ? effective : effective.withCompleting(now);
            if (!effective.isCompleting()) {
                boolean reserved = sessionPort.updateIfMatches(completing, effective.version(), effective.status());
                if (!reserved) {
                    Optional<UcpCheckoutSession> latest = sessionPort.findById(checkoutId);
                    if (latest.isPresent() && latest.get().isCompleted()) {
                        UcpCheckoutResponse response = buildCheckoutResponse(latest.get());
                        validateResponseSchema(response);
                        return response;
                    }
                    throw new UcpProtocolException(409, "conflict",
                            "Checkout session was modified concurrently", SEVERITY_ERROR, PATH_STATUS);
                }
            }

            List<OrderPlacementPort.PlaceCommand.Line> orderLines = completing.items().entrySet().stream()
                    .map(e -> new OrderPlacementPort.PlaceCommand.Line(e.getKey(), e.getValue()))
                    .toList();

            String paymentMethodId = resolvePaymentMethodId(completing);
            OrderPlacementPort.PlaceCommand.ShippingAddress shippingAddress = resolveShippingAddress(completing);

            OrderPlacementPort.PlaceCommand command = new OrderPlacementPort.PlaceCommand(
                    completing.userId(),
                    orderLines,
                    completing.voucherCode(),
                    paymentMethodId,
                    completing.currency(),
                    shippingAddress,
                    checkoutId);

            OrderPlacementPort.PlacedOrder placedOrder = orderPlacementPort.placeHeadless(command);

            Map<String, Long> completedPriceSnapshot = placedOrder.linePrices() != null
                    && !placedOrder.linePrices().isEmpty()
                            ? placedOrder.linePrices().entrySet().stream()
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> UcpCurrencyUtil.toMinorUnits(e.getValue(), placedOrder.currency()),
                                            (k1, k2) -> k1,
                                            LinkedHashMap::new))
                            : validated.priceSnapshot();

            UcpCheckoutSession completed = completing.withCompleted(placedOrder.orderId(), now, completedPriceSnapshot);
            boolean saved = sessionPort.updateIfMatches(completed, completing.version(), completing.status());
            if (!saved) {
                // CAS failed: associate the placed order with the latest session and complete it.
                Optional<UcpCheckoutSession> latest = sessionPort.findById(checkoutId);
                if (latest.isPresent() && latest.get().isCompleted()) {
                    UcpCheckoutResponse response = buildCheckoutResponse(latest.get());
                    validateResponseSchema(response);
                    return response;
                }
                UcpCheckoutSession recovered = latest
                        .map(s -> s.withCompleted(placedOrder.orderId(), now, completedPriceSnapshot))
                        .orElse(completed);
                sessionPort.save(recovered);
                UcpCheckoutResponse response = buildCheckoutResponse(recovered);
                validateResponseSchema(response);
                return response;
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

            if (existing.isCompleted() || existing.isCompleting()) {
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

    /**
     * Merges supplied Complete Checkout fields; absent fields keep stored values. The version is kept
     * because withCompleting bumps it in the same CAS write.
     */
    private UcpCheckoutSession mergeCompleteRequest(UcpCheckoutSession existing, UcpCheckoutRequest request,
            Instant now) {
        if (request == null) {
            return existing;
        }
        boolean hasUpdates = request.payment() != null
                || request.fulfillment() != null
                || request.buyer() != null
                || request.context() != null
                || request.voucherCode() != null;
        if (!hasUpdates) {
            return existing;
        }
        return new UcpCheckoutSession(
                existing.id(),
                existing.userId(),
                existing.cartId(),
                existing.status(),
                existing.currency(),
                existing.items(),
                request.buyer() != null ? request.buyer() : existing.buyer(),
                request.context() != null ? request.context() : existing.context(),
                existing.orderId(),
                existing.createdAt(),
                now,
                existing.expiresAt(),
                existing.priceSnapshot(),
                request.payment() != null ? request.payment() : existing.payment(),
                request.fulfillment() != null ? request.fulfillment() : existing.fulfillment(),
                request.voucherCode() != null ? request.voucherCode() : existing.voucherCode(),
                existing.version());
    }

    private String resolveVoucherCode(UcpCheckoutRequest request,
            CartOperationsPort.CartSnapshot cart) {
        if (request.voucherCode() != null && !request.voucherCode().isBlank()) {
            return request.voucherCode().trim();
        }
        return cart != null ? cart.voucherCode() : null;
    }

    /**
     * Resolves the payment method from the session's UCP payment block: the selected instrument's
     * {@code handler_id}, falling back to COD when none is supplied.
     */
    private String resolvePaymentMethodId(UcpCheckoutSession session) {
        Map<String, Object> payment = session.payment();
        if (payment == null) {
            return "cod";
        }
        Object instrumentsObj = payment.get("instruments");
        if (!(instrumentsObj instanceof List<?> instruments) || instruments.isEmpty()) {
            return "cod";
        }
        Map<String, Object> selected = null;
        for (Object o : instruments) {
            if (o instanceof Map<?, ?> values) {
                Map<String, Object> instrument = stringKeyedMap(values);
                Object sel = instrument.get("selected");
                if (Boolean.TRUE.equals(sel)) {
                    selected = instrument;
                    break;
                }
                if (selected == null) {
                    selected = instrument;
                }
            }
        }
        if (selected == null) {
            return "cod";
        }
        Object handlerId = selected.get("handler_id");
        if (handlerId instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        Object type = selected.get("type");
        if (type instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        return "cod";
    }

    /**
     * Resolves the destination from the first shipping method's {@code selected_destination_id},
     * falling back to its first destination. Returns null when no address was supplied.
     */
    private OrderPlacementPort.PlaceCommand.ShippingAddress resolveShippingAddress(UcpCheckoutSession session) {
        Map<String, Object> fulfillment = session.fulfillment();
        if (fulfillment == null) {
            return null;
        }
        Object methodsObj = fulfillment.get("methods");
        if (!(methodsObj instanceof List<?> methods)) {
            return null;
        }
        for (Object o : methods) {
            if (!(o instanceof Map<?, ?> values)) {
                continue;
            }
            Map<String, Object> method = stringKeyedMap(values);
            Object type = method.get("type");
            if (!(type instanceof String t) || !"shipping".equalsIgnoreCase(t)) {
                continue;
            }
            Map<String, Object> destination = pickDestination(method);
            if (destination != null) {
                return toShippingAddress(destination);
            }
        }
        return null;
    }

    private Map<String, Object> pickDestination(Map<String, Object> method) {
        Object destinationsObj = method.get("destinations");
        if (!(destinationsObj instanceof List<?> destinations) || destinations.isEmpty()) {
            return null;
        }
        Object selectedId = method.get("selected_destination_id");
        if (selectedId instanceof String sid && !sid.isBlank()) {
            for (Object destinationObject : destinations) {
                if (destinationObject instanceof Map<?, ?> values) {
                    Map<String, Object> destination = stringKeyedMap(values);
                    if (sid.equals(destination.get("id"))) {
                        return destination;
                    }
                }
            }
        }
        for (Object destinationObject : destinations) {
            if (destinationObject instanceof Map<?, ?> values) {
                return stringKeyedMap(values);
            }
        }
        return null;
    }

    private static Map<String, Object> stringKeyedMap(Map<?, ?> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key instanceof String name) {
                result.put(name, value);
            }
        });
        return result;
    }

    private OrderPlacementPort.PlaceCommand.ShippingAddress toShippingAddress(Map<String, Object> dest) {
        String contactName = joinNames(str(dest.get("first_name")), str(dest.get("last_name")));
        String region = str(dest.get("address_region"));
        String locality = str(dest.get("address_locality"));
        String country = str(dest.get("address_country"));
        String wardCode = locality;
        String districtCode = null;
        String provinceCode = region;

        if (addressLookupPort != null && isVietnam(country)) {
            if (region == null || locality == null) {
                throw new UcpProtocolException(400, "invalid_address",
                        "Shipping address requires address_region and address_locality",
                        SEVERITY_ERROR, "$.fulfillment.methods[].destinations[]");
            }
            var resolved = addressLookupPort.resolveByNames(country, region, locality);
            if (resolved.isEmpty()) {
                throw new UcpProtocolException(400, "invalid_address",
                        "Cannot resolve shipping province/locality to administrative codes: "
                                + region + " / " + locality,
                        SEVERITY_ERROR, "$.fulfillment.methods[].destinations[]");
            }
            wardCode = resolved.get().wardCode();
            districtCode = resolved.get().districtCode();
            provinceCode = resolved.get().provinceCode();
        }

        return new OrderPlacementPort.PlaceCommand.ShippingAddress(
                str(dest.get("id")),
                contactName,
                str(dest.get("phone_number")),
                buildDetailAddress(dest),
                wardCode,
                districtCode,
                provinceCode,
                country);
    }

    private static boolean isVietnam(String country) {
        if (country == null || country.isBlank()) {
            return true;
        }
        String normalized = country.trim().toLowerCase(java.util.Locale.ROOT);
        return "vn".equals(normalized) || "vietnam".equals(normalized) || "viet nam".equals(normalized);
    }

    private String buildDetailAddress(Map<String, Object> dest) {
        String street = str(dest.get("street_address"));
        String extended = str(dest.get("extended_address"));
        String postal = str(dest.get("postal_code"));
        StringBuilder sb = new StringBuilder();
        if (street != null) {
            sb.append(street);
        }
        if (extended != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(extended);
        }
        if (postal != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(postal);
        }
        return sb.length() > 0 ? sb.toString() : street;
    }

    private String joinNames(String first, String last) {
        if (first == null && last == null) {
            return null;
        }
        if (first == null) {
            return last;
        }
        if (last == null) {
            return first;
        }
        return first + " " + last;
    }

    private String str(Object o) {
        return o instanceof String s && !s.isBlank() ? s.trim() : null;
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
                throw new UcpProtocolException(400, "item_not_found",
                        "Item not found, inactive, or missing price: " + skuId,
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
            String resolvedCurrency = commonCurrency != null ? commonCurrency : protocolPolicy.marketCurrency();
            snapshot.put(skuId, UcpCurrencyUtil.toMinorUnits(pricing.price(), resolvedCurrency));
        }
        return new ValidatedPricing(commonCurrency != null ? commonCurrency : protocolPolicy.marketCurrency(),
                snapshot);
    }

    private Map<String, Integer> extractSkuQuantities(List<UcpLineItemRequest> lineItems) {
        if (lineItems.size() > MAX_LINE_ITEMS) {
            throw new UcpProtocolException(400, "invalid_request",
                    "Checkout must contain at most " + MAX_LINE_ITEMS + " line items", SEVERITY_ERROR, PATH_LINE_ITEMS);
        }
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

    private CheckoutQuotePort.Quote quoteCheckout(UcpCheckoutSession session) {
        if (checkoutQuotePort == null) {
            return null;
        }
        OrderPlacementPort.PlaceCommand.ShippingAddress destination;
        try {
            destination = resolveShippingAddress(session);
        } catch (RuntimeException ex) {
            // An incomplete address must not make a price preview fail; completion will validate
            // it again with a clear protocol error.
            return null;
        }
        if (destination == null) {
            return null;
        }
        List<CheckoutQuotePort.Line> quoteLines = session.items().entrySet().stream()
                .map(entry -> new CheckoutQuotePort.Line(entry.getKey(), entry.getValue()))
                .toList();
        try {
            CheckoutQuotePort.Quote quote = checkoutQuotePort.quote(new CheckoutQuotePort.QuoteRequest(
                    session.userId(),
                    quoteLines,
                    session.currency(),
                    session.voucherCode(),
                    new CheckoutQuotePort.ShippingAddress(
                            destination.addressId(),
                            destination.contactName(),
                            destination.phone(),
                            destination.detailAddress(),
                            destination.wardCode(),
                            destination.districtCode(),
                            destination.provinceCode(),
                            destination.countryCode())));
            if (quote == null || quote.currency() == null
                    || !quote.currency().equals(session.currency())
                    || quote.subtotalMinor() <= 0) {
                return null;
            }
            return quote;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private UcpCheckoutResponse buildCheckoutResponse(UcpCheckoutSession session) {
        List<String> skuIds = new ArrayList<>(session.items().keySet());
        Map<String, PricingQueryPort.SkuPricing> pricingMap = null;

        List<UcpLineItemResponse> lineItemResponses = new ArrayList<>();
        long calculatedSubtotal = 0;
        int itemIndex = 1;

        CheckoutQuotePort.Quote quote = quoteCheckout(session);
        Map<String, Long> quotedUnitPrices = quote == null ? Map.of()
                : quote.lines().stream().collect(Collectors.toMap(
                        CheckoutQuotePort.LinePrice::skuId,
                        CheckoutQuotePort.LinePrice::unitPriceMinor,
                        (first, second) -> first));

        for (Map.Entry<String, Integer> entry : session.items().entrySet()) {
            String skuId = entry.getKey();
            int qty = entry.getValue();

            long unitPriceMinor;
            Long quotedUnitPrice = quotedUnitPrices.get(skuId);
            if (quotedUnitPrice != null) {
                unitPriceMinor = quotedUnitPrice;
            } else if (session.priceSnapshot() != null && session.priceSnapshot().containsKey(skuId)) {
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

        long shippingMinor = quote == null ? 0L : quote.shippingMinor();
        long discountMinor = quote == null ? 0L : quote.discountMinor();
        long grandTotal = quote == null ? calculatedSubtotal : quote.totalMinor();

        List<UcpTotalResponse> checkoutTotals = new ArrayList<>();
        checkoutTotals.add(UcpTotalResponse.subtotal(calculatedSubtotal));
        if (discountMinor > 0) {
            checkoutTotals.add(UcpTotalResponse.discount(discountMinor));
        }
        if (shippingMinor > 0) {
            checkoutTotals.add(UcpTotalResponse.fulfillment(shippingMinor, "Shipping"));
        }
        checkoutTotals.add(UcpTotalResponse.total(grandTotal));

        List<UcpLinkResponse> links = List.of(
                UcpLinkResponse.termsOfService(protocolPolicy.storefrontBaseUrl()),
                UcpLinkResponse.privacyPolicy(protocolPolicy.storefrontBaseUrl()));

        UcpOrderConfirmationResponse orderConfirmation = session.orderId() != null
                ? UcpOrderConfirmationResponse.of(session.orderId(), protocolPolicy.storefrontBaseUrl())
                : null;

        UcpResponseMetadata ucpMetadata = UcpResponseMetadata.checkoutSuccess(UcpProtocolConstants.VERSION);

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
                session.payment(),
                session.fulfillment(),
                orderConfirmation,
                session.expiresAt() != null ? session.expiresAt().toString() : null,
                null);
    }

    private void validateResponseSchema(UcpCheckoutResponse response) {
        if (schemaValidator == null) {
            return;
        }
        try {
            schemaValidator.validate(UcpProtocolConstants.CHECKOUT_SCHEMA_URI, objectMapper.valueToTree(response));
        } catch (UcpProtocolException e) {
            throw e;
        } catch (Exception e) {
            throw new UcpProtocolException(500, "schema_validation_failed",
                    "Response schema validation error: " + e.getMessage(), SEVERITY_ERROR, null);
        }
    }
}
