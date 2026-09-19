package com.aionn.ucp.application.cart;

import com.aionn.sharedkernel.integration.port.catalog.CatalogQueryPort;
import com.aionn.sharedkernel.integration.port.catalog.PricingQueryPort;
import com.aionn.sharedkernel.integration.port.ordering.CartOperationsPort;
import com.aionn.sharedkernel.util.IdGenerator;
import com.aionn.ucp.adapter.rest.dto.cart.UcpCartRequest;
import com.aionn.ucp.adapter.rest.dto.cart.UcpCartResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpLineItemRequest;
import com.aionn.ucp.adapter.rest.dto.cart.UcpLineItemResponse;
import com.aionn.ucp.adapter.rest.dto.cart.UcpResponseMetadata;
import com.aionn.ucp.adapter.rest.dto.cart.UcpTotalResponse;
import com.aionn.ucp.application.port.out.UcpSchemaValidationPort;
import com.aionn.ucp.domain.exception.UcpProtocolException;
import com.aionn.ucp.domain.util.UcpCurrencyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UcpCartApplicationService {

    public static final String CART_SCHEMA_URI = "https://ucp.dev/schemas/shopping/cart.json";
    public static final String PROTOCOL_VERSION = "2026-08-25";
    private static final long CART_EXPIRY_DAYS = 7;

    private final CartOperationsPort cartPort;
    private final PricingQueryPort pricingPort;
    private final CatalogQueryPort catalogQueryPort;
    private final UcpSchemaValidationPort schemaValidator;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public UcpCartApplicationService(
            CartOperationsPort cartPort,
            PricingQueryPort pricingPort,
            CatalogQueryPort catalogQueryPort,
            UcpSchemaValidationPort schemaValidator,
            Clock clock) {
        this.cartPort = cartPort;
        this.pricingPort = pricingPort;
        this.catalogQueryPort = catalogQueryPort;
        this.schemaValidator = schemaValidator;
        this.clock = clock;
        this.objectMapper = new ObjectMapper();
    }

    public UcpCartResponse createCart(UcpCartRequest request, String authenticatedUserId) {
        validateLineItems(request.lineItems());

        String cartId = "cart_" + IdGenerator.ulid().toLowerCase();
        String userId = (authenticatedUserId != null && !authenticatedUserId.isBlank())
                ? authenticatedUserId
                : "ucp:guest:" + cartId;

        Map<String, Integer> skuQuantities = extractSkuQuantities(request.lineItems());
        CartOperationsPort.CartSnapshot saved = cartPort.saveCartItems(cartId, userId, skuQuantities);

        UcpCartResponse response = buildCartResponse(saved);
        validateResponseSchema(response);
        return response;
    }

    public UcpCartResponse getCart(String cartId, String authenticatedUserId) {
        CartOperationsPort.CartSnapshot snapshot = cartPort.findCartById(cartId)
                .orElseThrow(() -> new UcpProtocolException(404, "cart_not_found", "Cart not found: " + cartId, "error",
                        "$.id"));
        verifyOwnership(snapshot, authenticatedUserId);

        UcpCartResponse response = buildCartResponse(snapshot);
        validateResponseSchema(response);
        return response;
    }

    public UcpCartResponse updateCart(String cartId, UcpCartRequest request, String authenticatedUserId) {
        CartOperationsPort.CartSnapshot existing = cartPort.findCartById(cartId)
                .orElseThrow(() -> new UcpProtocolException(404, "cart_not_found", "Cart not found: " + cartId, "error",
                        "$.id"));
        verifyOwnership(existing, authenticatedUserId);

        validateLineItems(request.lineItems());

        Map<String, Integer> skuQuantities = extractSkuQuantities(request.lineItems());
        CartOperationsPort.CartSnapshot updated = cartPort.saveCartItems(cartId, existing.userId(), skuQuantities);

        UcpCartResponse response = buildCartResponse(updated);
        validateResponseSchema(response);
        return response;
    }

    public UcpCartResponse cancelCart(String cartId, String authenticatedUserId) {
        CartOperationsPort.CartSnapshot existing = cartPort.findCartById(cartId)
                .orElseThrow(() -> new UcpProtocolException(404, "cart_not_found", "Cart not found: " + cartId, "error",
                        "$.id"));
        verifyOwnership(existing, authenticatedUserId);

        CartOperationsPort.CartSnapshot cleared = cartPort.clearCart(cartId, existing.userId());

        UcpCartResponse response = buildCartResponse(cleared);
        validateResponseSchema(response);
        return response;
    }

    private void verifyOwnership(CartOperationsPort.CartSnapshot snapshot, String authenticatedUserId) {
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()) {
            if (!authenticatedUserId.equals(snapshot.userId())) {
                throw new UcpProtocolException(403, "access_denied", "You are not authorized to access this cart",
                        "error", "$.id");
            }
        } else if (!snapshot.userId().startsWith("ucp:guest:")) {
            throw new UcpProtocolException(403, "access_denied", "You are not authorized to access this cart", "error",
                    "$.id");
        }
    }

    private void validateLineItems(List<UcpLineItemRequest> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) {
            throw new UcpProtocolException(400, "invalid_request", "Cart must contain at least one line item", "error",
                    "$.line_items");
        }

        List<String> skuIds = new ArrayList<>();
        for (int i = 0; i < lineItems.size(); i++) {
            UcpLineItemRequest item = lineItems.get(i);
            if (item.item() == null || item.item().id() == null || item.item().id().isBlank()) {
                throw new UcpProtocolException(400, "invalid_item", "Item ID must not be blank", "error",
                        "$.line_items[" + i + "].item.id");
            }
            if (item.quantity() == null || item.quantity() < 1) {
                throw new UcpProtocolException(400, "invalid_quantity", "Quantity must be at least 1", "error",
                        "$.line_items[" + i + "].quantity");
            }
            skuIds.add(item.item().id());
        }

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
                            "All cart items must use the same currency. Found: " + commonCurrency + " and "
                                    + pricing.currency(),
                            "error", "$.line_items[" + i + "]");
                }
                commonCurrency = pricing.currency();
            }
        }
    }

    private Map<String, Integer> extractSkuQuantities(List<UcpLineItemRequest> lineItems) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (UcpLineItemRequest item : lineItems) {
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

    private UcpCartResponse buildCartResponse(CartOperationsPort.CartSnapshot snapshot) {
        List<String> skuIds = new ArrayList<>(snapshot.items().keySet());
        Map<String, PricingQueryPort.SkuPricing> pricingMap = skuIds.isEmpty() ? Map.of()
                : pricingPort.resolvePricing(skuIds);

        String currency = "USD";
        String resolvedCurrency = null;
        List<UcpLineItemResponse> lineItemResponses = new ArrayList<>();
        long calculatedSubtotal = 0;
        int itemIndex = 1;

        for (Map.Entry<String, Integer> entry : snapshot.items().entrySet()) {
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
                if (pricing.currency() != null) {
                    if (resolvedCurrency != null && !resolvedCurrency.equalsIgnoreCase(pricing.currency())) {
                        throw new UcpProtocolException(400, "mixed_currency_not_supported",
                                "Cart contains items with mixed currencies: " + resolvedCurrency + " and "
                                        + pricing.currency(),
                                "error", "$.line_items");
                    }
                    resolvedCurrency = pricing.currency();
                }
                unitPriceMinor = toMinorUnits(pricing.price(), resolvedCurrency != null ? resolvedCurrency : "USD");
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

        List<UcpTotalResponse> cartTotals = List.of(
                UcpTotalResponse.subtotal(calculatedSubtotal),
                UcpTotalResponse.total(calculatedSubtotal));

        UcpResponseMetadata ucpMetadata = UcpResponseMetadata.success(PROTOCOL_VERSION);

        Instant expiresAtInstant = (snapshot.createdAt() != null ? snapshot.createdAt() : clock.instant())
                .plus(CART_EXPIRY_DAYS, ChronoUnit.DAYS);

        if (resolvedCurrency != null) {
            currency = resolvedCurrency;
        }

        return new UcpCartResponse(
                ucpMetadata,
                snapshot.cartId(),
                currency,
                lineItemResponses,
                cartTotals,
                expiresAtInstant.toString(),
                null);
    }

    private long toMinorUnits(BigDecimal price, String currency) {
        return UcpCurrencyUtil.toMinorUnits(price, currency);
    }

    private void validateResponseSchema(UcpCartResponse response) {
        try {
            schemaValidator.validate(CART_SCHEMA_URI, objectMapper.valueToTree(response));
        } catch (Exception e) {
            // Re-throw if validation failed
            throw new UcpProtocolException(500, "schema_validation_error",
                    "Generated cart response failed schema validation: " + e.getMessage(), "error");
        }
    }
}
