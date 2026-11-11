package com.aionn.ucp.application;

/**
 * Pinned UCP wire constants. The protocol version and schema locations are part of the
 * contract published through discovery, not deployment configuration: changing them requires
 * a matching pinned schema bundle.
 */
public final class UcpProtocolConstants {

    public static final String VERSION = "2026-08-25";
    public static final String SCHEMA_PREFIX = "https://ucp.dev/schemas/";
    public static final String DEFAULT_MARKET_CURRENCY = "VND";

    public static final String CART_SCHEMA_URI = SCHEMA_PREFIX + "shopping/cart.json";
    public static final String CHECKOUT_SCHEMA_URI = SCHEMA_PREFIX + "shopping/checkout.json";
    public static final String ORDER_SCHEMA_URI = SCHEMA_PREFIX + "shopping/order.json";
    public static final String CATALOG_SEARCH_SCHEMA_URI =
            SCHEMA_PREFIX + "shopping/catalog_search.json#/$defs/search_response";
    public static final String CATALOG_LOOKUP_SCHEMA_URI =
            SCHEMA_PREFIX + "shopping/catalog_lookup.json#/$defs/lookup_response";
    public static final String GET_PRODUCT_SCHEMA_URI =
            SCHEMA_PREFIX + "shopping/catalog_lookup.json#/$defs/get_product_response";

    /** Idempotency window for state-changing UCP endpoints. */
    public static final int IDEMPOTENCY_TTL_SECONDS = 3600;

    private UcpProtocolConstants() {
    }
}
